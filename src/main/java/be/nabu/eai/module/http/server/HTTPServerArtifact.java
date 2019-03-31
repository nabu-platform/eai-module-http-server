package be.nabu.eai.module.http.server;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.RepositoryThreadFactory;
import be.nabu.eai.repository.api.LicenseManager;
import be.nabu.eai.repository.api.LicensedRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.artifacts.api.TunnelableArtifact;
import be.nabu.libs.artifacts.api.TwoPhaseStartableArtifact;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.HeaderMappingProvider;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.http.server.nio.HTTPPipelineFactoryImpl;
import be.nabu.libs.http.server.nio.NIOHTTPServer;
import be.nabu.libs.http.server.nio.RoutingMessageDataProvider;
import be.nabu.libs.nio.NIOServerUtils;
import be.nabu.libs.nio.api.ConnectionAcceptor;
import be.nabu.libs.nio.api.NIOServer;
import be.nabu.libs.nio.impl.MaxTotalConnectionsAcceptor;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.security.KeyStoreHandler;
import be.nabu.utils.security.SSLContextType;

public class HTTPServerArtifact extends JAXBArtifact<HTTPServerConfiguration> implements TwoPhaseStartableArtifact, StoppableArtifact, TunnelableArtifact {

	public static final String MODULE = "nabu.protocols.http.server";
	
	private static final String HTTP_IO_POOL_SIZE = "be.nabu.eai.http.ioPoolSize";
	private static final String HTTP_PROCESS_POOL_SIZE = "be.nabu.eai.http.processPoolSize";
	private Thread thread;
	private RoutingMessageDataProvider messageDataProvider;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public HTTPServerArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "httpServer.xml", HTTPServerConfiguration.class);
	}

	private NIOHTTPServer server;
	
	@Override
	public void stop() throws IOException {
		getServer().stop();
		// reset the server, we may want to restart it with different values? (e.g. the keystore has been updated)
		server = null;
		thread = null;
	}
	
	// whether communication with this http server is secured
	// note that the ssl might be terminated by a proxy
	public boolean isSecure() {
		return this.getConfig().isProxied() ? this.getConfig().isProxySecure() : this.getConfig().getKeystore() != null;
	}

	@Override
	public void start() throws IOException {
		if (getConfig().isEnabled()) {
			// build the server in the main thread to prevent classloader deadlocks cross thread
			final HTTPServer server = getServer();
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						server.start();
					}
					catch (IOException e) {
						logger.error("Could not start http server: " + getId(), e);
					}
				}
			});
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
		if (server == null) {
			synchronized(this) {
				if (server == null) {
					try {
						SSLContext context = null;
						Integer port = getConfiguration().getPort();
						if (getConfiguration().getKeystore() != null) {
							context = generateSecurityContext();
							if (port == null) {
								port = 443;
							}
						}
						else if (port == null) {
							port = 80;
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
						
						server = new NIOHTTPServer(
							context,
							getConfiguration().getSslServerMode(),
							port,
							ioPoolSize,
							processPoolSize,
							new EventDispatcherImpl(),
							new RepositoryThreadFactory(getRepository()),
							getConfig().isProxied(),
							new HeaderMappingProvider() {
								@Override
								public Map<String, String> getMappings() {
									return getConfig().isProxied() ? getConfig().getHeaderMapping() : null;
								}
							}
						);
						server.setMetrics(getRepository().getMetricInstance(getId()));
						server.setExceptionFormatter(new RepositoryExceptionFormatter(this));
						if (getConfiguration().getIdleTimeout() != null) {
							server.setMaxIdleTime(getConfiguration().getIdleTimeout());
						}
						if (getConfiguration().getLifetime() != null) {
							server.setMaxLifeTime(getConfiguration().getLifetime());
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
			return getConfig().getKeystore() != null ? 443 : 80;
		}
		else {
			return getConfig().getPort();
		}
	}

	@Override
	public void finish() {
		if (thread != null) {
			thread.start();
		}
	}

	@Override
	public boolean isFinished() {
		return isStarted() && thread.isAlive();
	}
}
