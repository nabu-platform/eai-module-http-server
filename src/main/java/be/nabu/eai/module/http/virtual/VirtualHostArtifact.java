/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.http.virtual;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.module.http.virtual.api.RequestRewriter;
import be.nabu.eai.module.http.virtual.api.RequestSubscriber;
import be.nabu.eai.module.http.virtual.api.ResponseRewriter;
import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.eai.server.Server;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.events.api.ResponseHandler;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.acme.AcmeClient;
import be.nabu.libs.http.acme.AcmeUtils;
import be.nabu.libs.http.acme.AcmeClient.ChallengeType;
import be.nabu.libs.http.acme.Challenge;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.LinkableHTTPResponse;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.client.nio.NIOHTTPClientImpl;
import be.nabu.libs.http.core.CustomCookieStore;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.core.HttpMessage;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.http.server.nio.MemoryMessageDataProvider;
import be.nabu.libs.http.server.util.RangeHandler;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.Pipeline;
import be.nabu.libs.nio.api.SourceContext;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.utils.cep.impl.CEPUtils;
import be.nabu.utils.cep.impl.HTTPComplexEventImpl;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;
import be.nabu.utils.security.KeyPairType;
import be.nabu.utils.security.SecurityUtils;
import be.nabu.utils.security.SignatureType;

public class VirtualHostArtifact extends JAXBArtifact<VirtualHostConfiguration> implements StartableArtifact, StoppableArtifact {

	private EventDispatcher dispatcher;
	private boolean started;
	private String originalKeyAlias;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public VirtualHostArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "virtual-host.xml", VirtualHostConfiguration.class);
	}
	
	public boolean isProxied() {
		HTTPServerArtifact server = getServer();
		if (server != null) {
			return server.getConfig().isProxied();
		}
		// we don't know at this point whether you are being proxied or not. we might in the future so we internalize it
		else if (getRepository().getServiceRunner() instanceof Server && getConfig().isInternalServer()) {
			return false;
		}
		return false;
	}
	
	public boolean isSecure() {
		HTTPServerArtifact server = getServer();
		if (server != null) {
			return server.isSecure();
		}
		// we don't know at this point whether you are being proxied or not. we might in the future so we internalize it
		else if (getRepository().getServiceRunner() instanceof Server && getConfig().isInternalServer()) {
			return false;
		}
		return false;
	}
	
	public Integer getPort() {
		HTTPServerArtifact server = getServer();
		if (server != null) {
			return server.getConfig().isProxied() ? server.getConfig().getProxyPort() : server.getConfig().getPort();
		}
		else if (getRepository().getServiceRunner() instanceof Server && getConfig().isInternalServer()) {
			return ((Server) getRepository().getServiceRunner()).getPort();
		}
		return null;
	}
	
	public HTTPServerArtifact getServer() {
		HTTPServerArtifact server = getConfig().getServer();
		int amount = 0;
		while (server != null && server.getConfig().getRedirectTo() != null && ++amount < 10) {
			server = server.getConfig().getRedirectTo();
		}
		if (amount == 10) {
			logger.error("Too many redirects to determine the correct server for host: " + getId());
		}
		return server;
	}
	
	public EventDispatcher getDispatcher() {
		if (dispatcher == null) {
			synchronized(this) {
				if (dispatcher == null) {
					EventDispatcher dispatcher = new EventDispatcherImpl();
					try {
						// make sure we redirect the user if he is on a redirect alias
						if (getConfig().getRedirectAliases() != null && !getConfig().getRedirectAliases().isEmpty()) {
							dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
								@Override
								public HTTPResponse handle(HTTPRequest event) {
									Header hostHeader = MimeUtils.getHeader("Host", event.getContent().getHeaders());
									if (hostHeader != null) {
										// make sure we remove any port reference
										String host = hostHeader.getValue().replaceAll(":[0-9]+$", "");
										String targetHost = null;
										// if it is a redirect host, we need to redirect...
										if (getConfig().getRedirectAliases().contains(host)) {
											targetHost = getConfig().getHost();
											// we first check if there is an actual alias that matches the current host
											// for instance if we redirect "www.example.com" to "example.com" or the reverse
											if (getConfig().getAliases() != null) {
												for (String alias : getConfig().getAliases()) {
													if (alias.contains(host) || host.contains(alias)) {
														targetHost = alias;
														break;
													}
												}
											}
										}
										// redirect!
										if (targetHost != null) {
											try {
												URI uri = HTTPUtils.getURI(event, getConfig().getServer().isSecure());
												URI redirect = new URI(uri.getScheme(), uri.getUserInfo(), targetHost, uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
												DefaultHTTPResponse response = new DefaultHTTPResponse(307, HTTPCodes.getMessage(307), new PlainMimeEmptyPart(null, 
													new MimeHeader("Content-Length", "0"),
													new MimeHeader("Location", redirect.toString())
												));
												response.setRequest(event);
												return response;
											}
											catch (Exception e) {
												throw new HTTPException(500, "Could not redirect user", e);
											}
										}
									}
									return null;
								}
							});
						}
						// allow request rewriting
						if (getConfiguration().getRequestRewriter() != null) {
							final RequestRewriter requestRewriter = POJOUtils.newProxy(RequestRewriter.class, getRepository(), SystemPrincipal.ROOT, getConfiguration().getRequestRewriter());
							dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPRequest>() {
								@Override
								public HTTPRequest handle(HTTPRequest event) {
									SourceContext sourceContext = PipelineUtils.getPipeline().getSourceContext();
									return requestRewriter.handle(new SourceImpl(sourceContext), event);
								}
							});
						}
						// allow request handlers
						if (getConfiguration().getRequestSubscriber() != null) {
							final RequestSubscriber requestSubscriber = POJOUtils.newProxy(RequestSubscriber.class, getRepository(), SystemPrincipal.ROOT, getConfiguration().getRequestSubscriber());
							dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
								@Override
								public HTTPResponse handle(HTTPRequest event) {
									SourceContext sourceContext = PipelineUtils.getPipeline().getSourceContext();
									return requestSubscriber.handle(new SourceImpl(sourceContext), event);
								}
							});
						}
						if (getConfiguration().getResponseRewriter() != null) {
							final ResponseRewriter responseSubscriber = POJOUtils.newProxy(ResponseRewriter.class, getRepository(), SystemPrincipal.ROOT, getConfiguration().getResponseRewriter());
							dispatcher.subscribe(HTTPResponse.class, new EventHandler<HTTPResponse, HTTPResponse>() {
								@Override
								public HTTPResponse handle(HTTPResponse event) {
									SourceContext sourceContext = PipelineUtils.getPipeline().getSourceContext();
									return responseSubscriber.handle(new SourceImpl(sourceContext), event);
								}
							});
						}
						// enable range support _before_ we enable the content encoding, otherwise it will be messed up
						if (getConfig().isEnableRangeSupport()) {
							dispatcher.subscribe(HTTPResponse.class, new RangeHandler());
						}
						// make sure we encode responses as much as possible
						if (!EAIResourceRepository.isDevelopment() && getConfig().isEnableCompression()) {
							dispatcher.subscribe(HTTPResponse.class, HTTPServerUtils.ensureContentEncoding());
						}
						
						// add a heartbeat
						if (getConfig().isHeartbeat()) {
							dispatcher.subscribe(HTTPRequest.class, new HeartbeatListener(this));
						}
						
						if (getConfig().isEnableHsts()) {
							EventSubscription<HTTPResponse, HTTPResponse> subscription = dispatcher.subscribe(HTTPResponse.class, new EventHandler<HTTPResponse, HTTPResponse>() {
								@Override
								public HTTPResponse handle(HTTPResponse event) {
									ModifiablePart content = event.getContent();
									// only inject the header if the server is secure
									if (content != null && getServer() != null && getServer().isSecure()) {
										Header header = MimeUtils.getHeader("Strict-Transport-Security", content.getHeaders());
										if (header == null) {
											long age = getConfig().getHstsMaxAge() == null ? 31536000 : getConfig().getHstsMaxAge();
											if (age < 0 || (age < 31536000 && getConfig().isHstsPreload())) {
												age = 31536000;
											}
											MimeHeader mimeHeader = new MimeHeader("Strict-Transport-Security", "max-age=" + age);
											if (getConfig().isHstsSubDomains() || getConfig().isHstsPreload()) {
												mimeHeader.addComment("includeSubDomains");
											}
											if (getConfig().isHstsPreload()) {
												mimeHeader.addComment("preload");
											}
											content.setHeader(mimeHeader);
										}
									}
									return null;
								}
							});
							// put it at the end for now
							// note that this is registered in the beginning however, other post processors will likely execute after this one, so the demote() has little added value for now
							subscription.demote();
						}
						// we always subscribe, other parts of the platform might indicate that logging is required
						if (getRepository().getComplexEventDispatcher() != null) {
							EventSubscription<HTTPResponse, HTTPResponse> subscription = dispatcher.subscribe(HTTPResponse.class, new EventHandler<HTTPResponse, HTTPResponse>() {
								@Override
								public HTTPResponse handle(HTTPResponse response) {
									if (response instanceof LinkableHTTPResponse) {
										HTTPRequest request = ((LinkableHTTPResponse) response).getRequest();
										if (request != null) {
											String artifactId = getId();
											boolean capture = (getConfig().isCaptureSuccessful() && response.getCode() < 400) || (getConfig().isCaptureErrors() && response.getCode() >= 400);
											// we can simply capture all at the virtual host level
											// or we can tell the virtual host to capture it
											Map<String, String> headerAsValues = MimeUtils.getHeaderAsValues("X-Nabu-Log", request.getContent().getHeaders());
											// the keys are lowercased
											if (!headerAsValues.isEmpty()) {
												if ("true".equalsIgnoreCase(headerAsValues.get("rawhttp"))) {
													capture = true;
												}
												if (headerAsValues.containsKey("artifactid")) {
													artifactId = headerAsValues.get("artifactid");
												}
											}
											if (capture) {
												HTTPComplexEventImpl event = new HTTPComplexEventImpl();
												event.setResponseCode(response.getCode());
												event.setMethod(request.getMethod());
												try {
													event.setRequestUri(HTTPUtils.getURI(request, isSecure()));
												}
												catch (FormatException e) {
													// best effort
													logger.warn("Could not format uri", e);
												}
												event.setArtifactId(artifactId);
												event.setEventCategory("http-message");
												Pipeline pipeline = PipelineUtils.getPipeline();
												CEPUtils.enrich(event, getClass(), "http-message-in", pipeline == null || pipeline.getSourceContext() == null ? null : pipeline.getSourceContext().getSocketAddress(), null, null);
												event.setApplicationProtocol("HTTP");
												event.setCorrelationId(MimeUtils.getCorrelationId(request.getContent().getHeaders()));
												HttpMessage messageIn = HTTPUtils.toMessage(request);
												HttpMessage messageOut = HTTPUtils.toMessage(response);
												event.setData("# Request\n\n" + messageIn.getMessage() + "\n\n# Response\n\n" + messageOut.getMessage());
												getRepository().getComplexEventDispatcher().fire(event, VirtualHostArtifact.this);
											}
										}
									}
									return null;
								}
							});
							subscription.demote();
						}
						this.dispatcher = dispatcher;
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return dispatcher;
	}
	
	@Override
	public void stop() throws IOException {
		if (started && getServer() != null) {
			if (getConfiguration().getHost() != null) {
				getServer().getServer().unroute(getConfiguration().getHost());
				if (getConfiguration().getAliases() != null) {
					for (String host : getConfiguration().getAliases()) {
						getServer().getServer().unroute(host);
					}
				}
			}
			else {
				getServer().getServer().unroute(null);
			}
		}
		if (getRepository().getServiceRunner() instanceof Server && getConfig().isInternalServer()) {
			HTTPServer server = ((Server) getRepository().getServiceRunner()).getHTTPServer();
			if (server != null) {
				// if it is named, we unroute it
				if (getConfiguration().getHost() != null) {
					server.unroute(getConfiguration().getHost());
					if (getConfiguration().getAliases() != null) {
						for (String host : getConfiguration().getAliases()) {
							server.unroute(host);
						}
					}
				}
				// we can't unroute everything that is null on the server
				// and we can't unroute this particular dispatcher
				// so instead, we shut the dispatcher off and set it to null, forcing a new dispatcher to be used upon reregister
				else {
					// filter all events
					// this is a known memory leak, but it should not be triggered often, minimizing the impact
					// if we have problems with this in the future we can extend either the dispatcher to allow unsubscribing all (reducing the memory leak) or update the server to unroute by dispatcher rather than host
					// perhaps the route() should return an unsubscriber handle?
					getDispatcher().filter(Object.class, new EventHandler<Object, Boolean>() {
						@Override
						public Boolean handle(Object event) {
							return true;
						}
					});
					// unset the dispatcher
					dispatcher = null;
				}
			}
		}
		started = false;
	}

	@Override
	public void start() throws IOException {
		// make sure we have an acme-issued certificate
		if (getConfig().isUseAcme()) {
			if (getConfig().getKeyAlias() == null || getConfig().getHost() == null) {
				throw new IOException("No key alias or host found for acme resolution");
			}
			if (originalKeyAlias == null) {
				originalKeyAlias = getConfig().getKeyAlias();
			}
			getConfig().setKeyAlias("acme-" + getConfig().getHost());
			validateAcme();
		}
		if (getServer() != null) {
			if (getConfiguration().getHost() != null && !getConfig().isDefaultHost()) {
				getServer().getServer().route(getConfiguration().getHost(), getDispatcher());
				if (getConfiguration().getAliases() != null) {
					for (String host : getConfiguration().getAliases()) {
						getServer().getServer().route(host, getDispatcher());
					}
				}
				if (getConfiguration().getRedirectAliases() != null) {
					for (String host : getConfiguration().getRedirectAliases()) {
						getServer().getServer().route(host, getDispatcher());
					}
				}
			}
			else {
				getServer().getServer().route(null, getDispatcher());
			}
		}
		if (getRepository().getServiceRunner() instanceof Server && getConfig().isInternalServer()) {
			final HTTPServer server = ((Server) getRepository().getServiceRunner()).getHTTPServer();
			if (server != null) {
				if (getConfiguration().getHost() != null) {
					server.route(getConfiguration().getHost(), getDispatcher());
					if (getConfiguration().getAliases() != null) {
						for (String host : getConfiguration().getAliases()) {
							server.route(host, getDispatcher());
						}
					}
					if (getConfiguration().getRedirectAliases() != null) {
						for (String host : getConfiguration().getRedirectAliases()) {
							server.route(host, getDispatcher());
						}
					}
				}
				else {
					server.getDispatcher(null).subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
						@Override
						public HTTPResponse handle(HTTPRequest event) {
							if (started) {
								return getDispatcher().fire(event, server, new ResponseHandler<HTTPRequest, HTTPResponse>() {
									@Override
									public HTTPResponse handle(HTTPRequest event, Object response, boolean isLast) {
										if (response instanceof HTTPResponse) {
											return (HTTPResponse) response;
										}
										else if (response instanceof Exception) {
											((Exception) response).printStackTrace();
											return new DefaultHTTPResponse(500, HTTPCodes.getMessage(500), new PlainMimeEmptyPart(null));
										}
										return null;
									}
								});
							}
							return null;
						}
					});
				}
			}
		}
		started = true;
	}
	
	@Deprecated
	public void validateAcme() {
		try {
			KeyStoreArtifact keystore = getConfig().getServer().getConfig().getKeystore();
			// if there is no key or the certificate is about to expire (about to = within 3 days), we start the procedure of updating it
			if (keystore.getKeyStore().getPrivateKey(getConfig().getKeyAlias()) == null || keystore.getKeyStore().getChain(getConfig().getKeyAlias())[0].getNotAfter().before(new Date(new Date().getTime() + 1000l*60*60*24*3))) {
				// the acme procedure expects a non-secure endpoint on the same domain where a token file can be retrieved
				// this means we need another virtual host that is linked to a server without a keystore on port 80
				VirtualHostArtifact unsecure = null;
				for (VirtualHostArtifact host : getRepository().getArtifacts(VirtualHostArtifact.class)) {
					if (!host.equals(this) && getConfig().getHost().equals(host.getConfig().getHost())) {
						if (host.getConfig().getServer() != null && host.getConfig().getServer().getConfig().getKeystore() == null) {
							Integer port = host.getConfig().getServer().getConfig().getPort();
							if (port == null || port == 80) {
								unsecure = host;
								break;
							}
						}
					}
				}
				if (unsecure == null) {
					throw new RuntimeException("No unsecure host found for the domain: " + getConfig().getHost());
				}
				
				logger.info("Starting acme procedure for: " + getId());

				boolean staging = Boolean.parseBoolean(System.getProperty("acme.staging", "false"));
				String uri = staging ? AcmeClient.LETS_ENCRYPT_STAGING : AcmeClient.LETS_ENCRYPT;
				logger.info("Acme directory endpoint: " + uri);
				
				// we use the original pair as the user pair
				X509Certificate originalCertificate = getConfig().getServer().getConfig().getKeystore().getKeyStore().getChain(originalKeyAlias)[0];
				KeyPair user = new KeyPair(
					originalCertificate.getPublicKey(),
					getConfig().getServer().getConfig().getKeystore().getKeyStore().getPrivateKey(originalKeyAlias)
				);
				AcmeClient acmeClient = new AcmeClient(new NIOHTTPClientImpl(null, 5, 5, 5, new be.nabu.libs.events.impl.EventDispatcherImpl(), new MemoryMessageDataProvider(), 
					new CookieManager(new CustomCookieStore(), CookiePolicy.ACCEPT_NONE), Executors.defaultThreadFactory()),
					user, 
					new URI(uri));
				
				final Challenge challenge = acmeClient.challenge(getConfig().getHost(), ChallengeType.HTTP);
				if (challenge != null) {
					logger.debug("Received challenge: " + challenge.getToken());
					logger.debug("Subscribing to: " + unsecure.getConfig().getHost() + " @ "+ unsecure.getConfig().getServer().getId() + " / " + unsecure.isStarted());
					final String path = AcmeUtils.getHttpUri(challenge).getPath();
					EventSubscription<HTTPRequest, HTTPResponse> subscription = unsecure.getDispatcher().subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
						@Override
						public HTTPResponse handle(HTTPRequest request) {
							try {
								URI uri = HTTPUtils.getURI(request, false);
								logger.debug("Checking for challenge match: " + uri.getPath() + " == " + path);
								if (uri.getPath().equals(path)) {
									byte [] content = challenge.getAuthorization().getBytes(Charset.forName("UTF-8"));
									return new DefaultHTTPResponse(200, HTTPCodes.getMessage(200), new PlainMimeContentPart(null, 
										IOUtils.wrap(content, true), 
										new MimeHeader("Content-Length", "" + content.length)));
								}
							}
							catch (Exception e) {
								throw new HTTPException(500, e);
							}
							return null;
						}
					});
					subscription.promote();
					// we tell the server that the challenge is ready
					try {
						challenge.accept();
					}
					// unregister the subscription either way
					finally {
						subscription.unsubscribe();
					}
				}
				if (staging) {
					boolean success = challenge == null || challenge.accepted();
					if (success) {
						logger.info("ACME Staging challenge for '" + unsecure.getConfig().getHost() + "' successful");
					}
					else {
						logger.warn("ACME Staging challenge for '" + unsecure.getConfig().getHost() + "' failed");
					}
				}
				else if (challenge == null || challenge.accepted()) {
					KeyPair pair = SecurityUtils.generateKeyPair(KeyPairType.RSA, 4096);
					// we take one day in the past to bypass any possible timezone issues
					X509Certificate [] chain = acmeClient.certify(getConfig().getHost(), pair, SignatureType.SHA256WITHRSA, originalCertificate.getSubjectX500Principal(), 
						// not before (yesterday)
						new Date(new Date().getTime() - 1000l*60*60*24), 
						// not after (one year)
						new Date(new Date().getTime() + 1000l*60*60*24*365)
					);
					getConfig().getServer().getConfig().getKeystore().getKeyStore().set("acme-" + getConfig().getHost(), pair.getPrivate(), chain, null);
					ResourceContainer<?> directory = getConfig().getServer().getConfig().getKeystore().getDirectory();
					if (directory != null) {
						try {
							getConfig().getServer().getConfig().getKeystore().save(directory);
						}
						catch (Exception e) {
							logger.warn("Could not persist acme key, it will still be used but will have to be re-requested upon reboot", e);
						}
					}
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isStarted() {
		return started;
	}

}
