package be.nabu.eai.module.http.server;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.RepositoryThreadFactory;
import be.nabu.eai.repository.api.LicenseManager;
import be.nabu.eai.repository.api.LicensedRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.server.Server;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.artifacts.api.TunnelableArtifact;
import be.nabu.libs.artifacts.api.TwoPhaseOfflineableArtifact;
import be.nabu.libs.artifacts.api.TwoPhaseStartableArtifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.api.EventTarget;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.HeaderMappingProvider;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.core.ServerHeader;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.http.server.nio.HTTPPipelineFactoryImpl;
import be.nabu.libs.http.server.nio.NIOHTTPServer;
import be.nabu.libs.http.server.nio.RoutingMessageDataProvider;
import be.nabu.libs.nio.NIOServerUtils;
import be.nabu.libs.nio.api.ConnectionAcceptor;
import be.nabu.libs.nio.api.NIOServer;
import be.nabu.libs.nio.impl.MaxTotalConnectionsAcceptor;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.cep.impl.ComplexEventImpl;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;
import be.nabu.utils.security.KeyStoreHandler;
import be.nabu.utils.security.SSLContextType;
/*
 * two offline modes supported: 
 * - round robin: we switch to another port an keep entire application alive (this assumes with load balancer ignoring "down" server)
 * 		we can still spotcheck and validate the application on the other port
 * 		if done well, the application is never down
 * - all or nothing: the server stays available on the default port, but any application on top of it starts sending back 503 (unless specifically bypassing offline mode)
 * 		the frontend can then switch to showing a proper "offline" message
 */
public class HTTPServerArtifact extends JAXBArtifact<HTTPServerConfiguration> implements TwoPhaseStartableArtifact, StoppableArtifact, TunnelableArtifact, TwoPhaseOfflineableArtifact {

	public static final String MODULE = "nabu.protocols.http.server";
	
	public static final String HTTP_IO_POOL_SIZE = "be.nabu.eai.http.ioPoolSize";
	public static final String HTTP_PROCESS_POOL_SIZE = "be.nabu.eai.http.processPoolSize";
	private Thread thread;
	private RoutingMessageDataProvider messageDataProvider;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public HTTPServerArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "httpServer.xml", HTTPServerConfiguration.class);
	}

	private NIOHTTPServer server;
	
	@Override
	public void stop() throws IOException {
		// don't use getServer here! otherwise we go into reload loop
		if (server != null) {
			server.stop();
		}
		// reset the server, we may want to restart it with different values? (e.g. the keystore has been updated)
		server = null;
		thread = null;
	}
	
	// whether communication with this http server is secured
	// note that the ssl might be terminated by a proxy
	public boolean isSecure() {
		return this.getConfig().isProxied() ? this.getConfig().isProxySecure() : this.getConfig().getKeystore() != null;
	}
	
	// similar to is secure but it checks if THIS server is doing the terminating
	// this can be relevant for example to see if we want ACME
	public boolean isSecuring() {
		return this.getConfig().getKeystore() != null;
	}

	@Override
	public void start() throws IOException {
		start(false);
	}

	private void start(final boolean restartHosts) throws IOException {
		// always stop first
		stop();
		// if enabled, we start
		if (getConfig().isEnabled()) {
			// build the server in the main thread to prevent classloader deadlocks cross thread
			final HTTPServer server = getServer(false);
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					if (restartHosts) {
						restartHosts();
					}
					try {
						server.start();
					}
					catch (IOException e) {
						logger.error("Could not start http server: " + getId(), e);
					}
				}
			});
			thread.setName(getId());
		}
	}
	
	public void updateSecurityContext() {
		if (getConfig().getKeystore() != null && server != null) {
			try {
				server.setSSLContext(generateSecurityContext());
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public HTTPServer getServer() {
		boolean offline = false;
		// if we have an offline port, we can have two modes, otherwise we can't!
		if (getConfig().getOfflinePort() != null && getRepository().getServiceRunner() instanceof Server) {
			offline = ((Server) getRepository().getServiceRunner()).isOffline();
		}
		return getServer(offline);
	}
	
	private HTTPServer getServer(boolean offline) {
		Integer port = offline && getConfig().getOfflinePort() != null ? getConfig().getOfflinePort() : getConfig().getPort();
		// if the port is null, we don't have an offline port (or it is not in offline mode), so we are definitely online but don't have a port
		if (getConfig().getKeystore() != null) {
			if (port == null) {
				port = 443;
			}
		}
		else if (port == null) {
			port = 80;
		}
		if (server != null && server.getPort() != port) {
			try {
				stop();
			}
			catch (IOException e) {
				logger.error("Could not stop the server", e);
			}
		}
		if (server == null) {
			synchronized(this) {
				if (server == null) {
					try {
						SSLContext context = null;
						if (getConfig().getKeystore() != null) {
							context = generateSecurityContext();
						}
						Integer ioPoolSize = getConfiguration().getIoPoolSize() == null ? new Integer(System.getProperty(HTTP_IO_POOL_SIZE, "5")) : getConfiguration().getIoPoolSize();
						Integer processPoolSize = getConfiguration().getPoolSize() == null ? new Integer(System.getProperty(HTTP_PROCESS_POOL_SIZE, "10")) : getConfiguration().getPoolSize();
						
						if (getRepository() instanceof LicensedRepository) {
							LicenseManager manager = ((LicensedRepository) getRepository()).getLicenseManager();
							if (manager == null || !manager.isLicensed(MODULE)) {
								ioPoolSize = Math.min(2, ioPoolSize);
								processPoolSize = Math.min(5, processPoolSize);
								logger.warn("No license found for the http server module, it is running with reduced capacity");
							}
						}
						
						RepositoryThreadFactory threadFactory = new RepositoryThreadFactory(getRepository());
						threadFactory.setName(getId());
						server = new NIOHTTPServer(
							context,
							getConfiguration().getSslServerMode(),
							port,
							ioPoolSize,
							processPoolSize,
							new EventDispatcherImpl(),
							threadFactory,
							getConfig().isProxied(),
							new HeaderMappingProvider() {
								@Override
								public Map<String, String> getMappings() {
									if (getConfig().isProxied()) {
										Map<String, String> headerMapping = getConfig().getHeaderMapping();
										if (headerMapping == null) {
											headerMapping = new HashMap<String, String>();
										}
										// if we are behind a nabu proxy, add the default mappings
										if (getConfig().isNabuProxy()) {
											for (ServerHeader header : ServerHeader.values()) {
												// you did not fill in something manually
												if (headerMapping.get(header.getName()) == null) {
													headerMapping.put(header.getName(), header.getName());
												}
											}
										}
										return headerMapping;
									}
									return null;
								}
							},
							new EventTarget() {
								@Override
								public <E> void fire(E event, Object source) {
									if (getRepository().getComplexEventDispatcher() != null) {
										// set the id of this server
										if (event instanceof ComplexEventImpl) {
											((ComplexEventImpl) event).setArtifactId(getId());
										}
										getRepository().getComplexEventDispatcher().fire(event, source);
									}
								}
							}
						);
						server.setMetrics(getRepository().getMetricInstance(getId()));
						server.setExceptionFormatter(new RepositoryExceptionFormatter(this));
						if (getConfiguration().getIdleTimeout() != null) {
							server.setMaxIdleTime(getConfiguration().getIdleTimeout());
						}
						// if we are proxied, it is up to the proxy to decide
						else if (getConfig().isProxied()) {
							server.setMaxIdleTime(0l);
						}
						if (getConfiguration().getLifetime() != null) {
							server.setMaxLifeTime(getConfiguration().getLifetime());
						}
						// if we are proxied, it is up to the proxy to decide
						else if (getConfig().isProxied()) {
							server.setMaxLifeTime(0l);
						}
						// make sure we encode responses as much as possible
						if (!EAIResourceRepository.isDevelopment()) {
							server.getDispatcher().subscribe(HTTPResponse.class, HTTPServerUtils.ensureContentEncoding());
						}
						if (getConfiguration().getReadTimeout() != null) {
							((NIOHTTPServer) server).getPipelineFactory().setReadTimeout(getConfiguration().getReadTimeout());
						}
						if (getConfiguration().getWriteTimeout() != null) {
							((NIOHTTPServer) server).getPipelineFactory().setReadTimeout(getConfiguration().getWriteTimeout());
						}
						if (getConfiguration().getRequestLimit() != null) {
							((NIOHTTPServer) server).getPipelineFactory().setRequestLimit(getConfiguration().getRequestLimit());
						}
						if (getConfiguration().getResponseLimit() != null) {
							((NIOHTTPServer) server).getPipelineFactory().setResponseLimit(getConfiguration().getResponseLimit());
						}
						// set limits
						if (getConfig().getMaxChunkSize() != null) {
							((HTTPPipelineFactoryImpl) server.getPipelineFactory()).setMaxChunkSize(getConfig().getMaxChunkSize());
						}
						if (getConfig().getMaxHeaderSize() != null) {
							((HTTPPipelineFactoryImpl) server.getPipelineFactory()).setMaxHeaderSize(getConfig().getMaxHeaderSize());
						}
						if (getConfig().getMaxInitialLineLength() != null) {
							((HTTPPipelineFactoryImpl) server.getPipelineFactory()).setMaxInitialLineLength(getConfig().getMaxInitialLineLength());
						}
						// add connection restrictions, the 6 connections is the default for firefox & chrome
						// IE10 apparently has 8
						ConnectionAcceptor connectionAcceptor = null;
						if (getConfiguration().getMaxConnectionsPerClient() != null) {
							connectionAcceptor = NIOServerUtils.maxConnectionsPerClient(getConfiguration().getMaxConnectionsPerClient());
						}
						if (getConfiguration().getMaxTotalConnections() != null) {
							MaxTotalConnectionsAcceptor maxTotalConnectionsAcceptor = new MaxTotalConnectionsAcceptor(getConfiguration().getMaxTotalConnections());
							connectionAcceptor = connectionAcceptor == null ? maxTotalConnectionsAcceptor : NIOServerUtils.combine(maxTotalConnectionsAcceptor, connectionAcceptor);
						}
						if (connectionAcceptor != null) {
							((NIOServer) server).setConnectionAcceptor(connectionAcceptor);
						}
						if (getConfiguration().getMaxSizePerRequest() != null) {
							messageDataProvider = new RoutingMessageDataProvider(getConfiguration().getMaxSizePerRequest());
						}
						else {
							messageDataProvider = new RoutingMessageDataProvider();
						}
						server.setMessageDataProvider(messageDataProvider);
						
						final HTTPServerArtifact redirectTo = getConfig().getRedirectTo();
						if (redirectTo != null) {
							EventDispatcherImpl redirectDispatcher = new EventDispatcherImpl();
							redirectDispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
								@Override
								public HTTPResponse handle(HTTPRequest event) {
									try {
										URI uri = HTTPUtils.getURI(event, isSecure());
										int port = redirectTo.getConfig().getPort() == null ? -1 : redirectTo.getConfig().getPort();
										URI newUri = new URI(redirectTo.isSecure() ? "https" : "http", null, uri.getHost(), port, uri.getPath(), uri.getQuery(), uri.getFragment());
										return new DefaultHTTPResponse(307, HTTPCodes.getMessage(307), new PlainMimeEmptyPart(null, 
											new MimeHeader("Content-Length", "0"),
											new MimeHeader("Location", newUri.toString()))
										);
									}
									catch (Exception e) {
										logger.error("Can not redirect user", e);
										return null;
									}
								}
							});
							server.route(null, redirectDispatcher);
						}
						
					}
					catch (KeyManagementException e) {
						throw new RuntimeException(e);
					}
					catch (UnrecoverableKeyException e) {
						throw new RuntimeException(e);
					}
					catch (KeyStoreException e) {
						throw new RuntimeException(e);
					}
					catch (NoSuchAlgorithmException e) {
						throw new RuntimeException(e);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return server;
	}

	private SSLContext generateSecurityContext() throws IOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyManagementException {
		KeyStoreHandler keyStoreHandler = new KeyStoreHandler(getConfiguration().getKeystore().getKeyStore().getKeyStore());
		KeyManager[] keyManagers = keyStoreHandler.getKeyManagers();
		for (int i = 0; i < keyManagers.length; i++) {
			if (keyManagers[i] instanceof X509KeyManager) {
				keyManagers[i] = new ArtifactAwareKeyManager((X509KeyManager) keyManagers[i], getRepository(), this);
			}
		}
		SSLContext context = SSLContext.getInstance(SSLContextType.TLS.toString());
		context.init(keyManagers, keyStoreHandler.getTrustManagers(), new SecureRandom());
		return context;
	}

	@Override
	public boolean isStarted() {
		return thread != null && thread.getState() != Thread.State.TERMINATED;
	}

	public RoutingMessageDataProvider getMessageDataProvider() {
		return messageDataProvider;
	}

	@Override
	public String getTunnelHost() {
		// it is always running on the current host
		return "localhost";
	}

	@Override
	public Integer getTunnelPort() {
		if (getConfig().getPort() == null) {
			return isSecuring() ? 443 : 80;
		}
		else {
			return getConfig().getPort();
		}
	}

	@Override
	public void finish() {
		// start if not yet started
		if (thread != null && thread.getState() == Thread.State.NEW) {
			thread.start();
		}
	}

	@Override
	public boolean isFinished() {
		return isStarted() && thread.isAlive();
	}

	@Override
	public void online() throws IOException {
		// if we are running on the "offline port", we need to switch to the main port
		// otherwise we assume the server is simply running correctly
		if (getConfig().getOfflinePort() != null) {
			start(true);
		}
	}

	@Override
	public void offline() throws IOException {
		// if we have an offline port, we need to switch from the main to that one
		if (getConfig().getOfflinePort() != null && getConfig().isEnabled()) {
			// build the server in the main thread to prevent classloader deadlocks cross thread
			final HTTPServer server = getServer(true);
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					// restart the hosts so they hook up to this http server now
					restartHosts();
					try {
						server.start();
					}
					catch (IOException e) {
						logger.error("Could not start http server: " + getId(), e);
					}
				}
			});
			thread.setName(getId());
		}
	}
	
	// if we play around with the underlying http server (e.g. for online/offline cycles) we should restart the virtual hosts so they can re-register
	private void restartHosts() {
		for (VirtualHostArtifact host : getRepository().getArtifacts(VirtualHostArtifact.class)) {
			HTTPServerArtifact serverArtifact = host.getConfig().getServer();
			// bypass equality of objects, go straight for id
			if (serverArtifact != null && serverArtifact.getId().equals(getId())) {
				try {
					if (host.isStarted()) {
						host.stop();
					}
					host.start();
				}
				catch (Exception e) {
					logger.error("Could not restart virtual host: " + host.getId());
				}
			}
		}
	}

	@Override
	public void startOffline() throws IOException {
		if (getConfig().isEnabled()) {
			// start on the offline port
			if (getConfig().getOfflinePort() != null) {
				offline();
			}
			// start on the main port
			else {
				start();
			}
		}
	}

	@Override
	public void onlineFinish() {
		// when we come online, do the finishing!
		finish();
	}

	@Override
	public void offlineFinish() {
		// when we go offline, do the finishing as well
		finish();
	}
	
}
