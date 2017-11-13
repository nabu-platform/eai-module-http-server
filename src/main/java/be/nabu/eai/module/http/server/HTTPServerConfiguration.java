package be.nabu.eai.module.http.server;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.io.SSLServerMode;

@XmlRootElement(name = "httpServer")
@XmlType(propOrder = { "enabled", "keystore", "sslServerMode", "port", "poolSize", "ioPoolSize", "maxTotalConnections", "maxConnectionsPerClient", "maxSizePerRequest", "idleTimeout", "lifetime", "readTimeout", "writeTimeout", "requestLimit", "responseLimit", "maxInitialLineLength", "maxHeaderSize", "maxChunkSize", "proxied" })
public class HTTPServerConfiguration {
	private Integer port;
	private KeyStoreArtifact keystore;
	private SSLServerMode sslServerMode;
	private Integer poolSize, ioPoolSize, maxTotalConnections, maxConnectionsPerClient;
	private Long maxSizePerRequest;
	private Long readTimeout, writeTimeout, idleTimeout, lifetime;
	private Integer requestLimit, responseLimit;
	private boolean enabled = true, proxied;
	private Integer maxInitialLineLength;
	private Integer maxHeaderSize;
	private Integer maxChunkSize;
	
	@Comment(title = "The port that the server will be listening on")
	@EnvironmentSpecific
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	
	@Comment(title = "You can enable ssl by configuring a keystore here and setting the alias to the correct key on each virtual host")
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getKeystore() {
		return keystore;
	}
	public void setKeystore(KeyStoreArtifact keystore) {
		this.keystore = keystore;
	}
	
	@Comment(title = "The amount of threads that the server has to process incoming requests, the default is 10", description = "The default can be overwritten using the system property 'be.nabu.eai.http.processPoolSize'")
	@EnvironmentSpecific
	public Integer getPoolSize() {
		return poolSize;
	}
	public void setPoolSize(Integer poolSize) {
		this.poolSize = poolSize;
	}
	
	@Comment(title = "This option lets you decide what to do with client certificates.")
	@Advanced
	@EnvironmentSpecific
	public SSLServerMode getSslServerMode() {
		return sslServerMode;
	}
	public void setSslServerMode(SSLServerMode sslServerMode) {
		this.sslServerMode = sslServerMode;
	}
	
	@Comment(title = "The amount of threads the server has to communicate with the client (both incoming and outgoing), the default is 5", description = "The default can be overwritten using the system property 'be.nabu.eai.http.ioPoolSize'")
	@EnvironmentSpecific
	public Integer getIoPoolSize() {
		return ioPoolSize;
	}
	public void setIoPoolSize(Integer ioPoolSize) {
		this.ioPoolSize = ioPoolSize;
	}
	
	@Advanced
	@Comment(title = "The maximum amount of connections this server will accept, the default is unlimited")
	@EnvironmentSpecific
	public Integer getMaxTotalConnections() {
		return maxTotalConnections;
	}
	public void setMaxTotalConnections(Integer maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}
	
	@Advanced
	@Comment(title = "The maximum amount of connections this server will accept per client, the default is unlimited")
	@EnvironmentSpecific
	public Integer getMaxConnectionsPerClient() {
		return maxConnectionsPerClient;
	}
	public void setMaxConnectionsPerClient(Integer maxConnectionsPerClient) {
		this.maxConnectionsPerClient = maxConnectionsPerClient;
	}
	
	@Comment(title = "The maximum size of an incoming request in bytes, anything above this will be cut off")
	@Advanced
	public Long getMaxSizePerRequest() {
		return maxSizePerRequest;
	}
	public void setMaxSizePerRequest(Long maxSizePerRequest) {
		this.maxSizePerRequest = maxSizePerRequest;
	}
	
	@Advanced
	@Comment(title = "The maximum amount of time it can take for a single request to make it to the server, measured from the arrival of the first byte. The default is unlimited.")
	@EnvironmentSpecific
	public Long getReadTimeout() {
		return readTimeout;
	}
	public void setReadTimeout(Long readTimeout) {
		this.readTimeout = readTimeout;
	}
	
	@Advanced
	@Comment(title = "The maximum amount of time it can take for a single response to make it to the client, measured from the departure of the first byte. The default is unlimited.")
	@EnvironmentSpecific
	public Long getWriteTimeout() {
		return writeTimeout;
	}
	public void setWriteTimeout(Long writeTimeout) {
		this.writeTimeout = writeTimeout;
	}
	
	@Advanced
	@Comment(title = "The maximum amount of requests that can be queued for processing for a given connection. The default is unlimited.")
	@EnvironmentSpecific
	public Integer getRequestLimit() {
		return requestLimit;
	}
	public void setRequestLimit(Integer requestLimit) {
		this.requestLimit = requestLimit;
	}
	
	@Comment(title = "The maximum amount of responses that can be queued for a given connection. The default is unlimited.")
	@Advanced
	@EnvironmentSpecific
	public Integer getResponseLimit() {
		return responseLimit;
	}
	public void setResponseLimit(Integer responseLimit) {
		this.responseLimit = responseLimit;
	}
	
	@Comment(title = "How long a connection can be idle before it is disconnected by the server, the default is 5 minutes. If set to 0 it will never time out.")
	public Long getIdleTimeout() {
		return idleTimeout;
	}
	public void setIdleTimeout(Long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
	
	@Comment(title = "How long a connection (even an active one) can exist before it is disconnected by the server, the default is 1 hour. If set to 0 it will never be disconnected.")
	public Long getLifetime() {
		return lifetime;
	}
	public void setLifetime(Long lifetime) {
		this.lifetime = lifetime;
	}
	
	@EnvironmentSpecific
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Advanced
	public Integer getMaxInitialLineLength() {
		return maxInitialLineLength;
	}
	public void setMaxInitialLineLength(Integer maxInitialLineLength) {
		this.maxInitialLineLength = maxInitialLineLength;
	}
	
	@Advanced
	public Integer getMaxHeaderSize() {
		return maxHeaderSize;
	}
	public void setMaxHeaderSize(Integer maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
	}
	
	@Advanced
	public Integer getMaxChunkSize() {
		return maxChunkSize;
	}
	public void setMaxChunkSize(Integer maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}
	
	@EnvironmentSpecific
	@Advanced
	public boolean isProxied() {
		return proxied;
	}
	public void setProxied(boolean proxied) {
		this.proxied = proxied;
	}
	
}
