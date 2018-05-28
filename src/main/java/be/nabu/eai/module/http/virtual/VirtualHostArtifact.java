package be.nabu.eai.module.http.virtual;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.http.virtual.api.RequestRewriter;
import be.nabu.eai.module.http.virtual.api.RequestSubscriber;
import be.nabu.eai.module.http.virtual.api.ResponseRewriter;
import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.acme.AcmeClient;
import be.nabu.libs.http.acme.AcmeUtils;
import be.nabu.libs.http.acme.AcmeClient.ChallengeType;
import be.nabu.libs.http.acme.Challenge;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.client.nio.NIOHTTPClientImpl;
import be.nabu.libs.http.core.CustomCookieStore;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.server.nio.MemoryMessageDataProvider;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.SourceContext;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
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

	public EventDispatcher getDispatcher() {
		if (dispatcher == null) {
			synchronized(this) {
				if (dispatcher == null) {
					EventDispatcher dispatcher = new EventDispatcherImpl();
					try {
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
		if (started && getConfiguration().getServer() != null) {
			if (getConfiguration().getHost() != null) {
				getConfiguration().getServer().getServer().unroute(getConfiguration().getHost());
				if (getConfiguration().getAliases() != null) {
					for (String host : getConfiguration().getAliases()) {
						getConfiguration().getServer().getServer().unroute(host);
					}
				}
			}
			else {
				getConfiguration().getServer().getServer().unroute(null);
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
		if (getConfiguration().getServer() != null) {
			if (getConfiguration().getHost() != null) {
				getConfiguration().getServer().getServer().route(getConfiguration().getHost(), getDispatcher());
				if (getConfiguration().getAliases() != null) {
					for (String host : getConfiguration().getAliases()) {
						getConfiguration().getServer().getServer().route(host, getDispatcher());
					}
				}
			}
			else {
				getConfiguration().getServer().getServer().route(null, getDispatcher());
			}
			started = true;
		}
	}
	
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
