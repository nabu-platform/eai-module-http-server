package be.nabu.eai.module.http.server;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.io.SSLServerMode;

@XmlRootElement(name = "httpServer")
@XmlType(propOrder = { "keystore", "sslServerMode", "port", "poolSize", "ioPoolSize", "maxTotalConnections", "maxConnectionsPerClient", "maxSizePerRequest", "idleTimeout", "lifetime", "readTimeout", "writeTimeout", "requestLimit", "responseLimit" })
public class HTTPServerConfiguration {
	private Integer port;
	private KeyStoreArtifact keystore;
	private SSLServerMode sslServerMode;
	private Integer poolSize, ioPoolSize, maxTotalConnections, maxConnectionsPerClient;
	private Long maxSizePerRequest;
	private Long readTimeout, writeTimeout, idleTimeout, lifetime;
	private Integer requestLimit, responseLimit;
	
	@EnvironmentSpecific
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getKeystore() {
		return keystore;
	}
	public void setKeystore(KeyStoreArtifact keystore) {
		this.keystore = keystore;
	}
	
	@EnvironmentSpecific
	public Integer getPoolSize() {
		return poolSize;
	}
	public void setPoolSize(Integer poolSize) {
		this.poolSize = poolSize;
	}
	
	@EnvironmentSpecific
	public SSLServerMode getSslServerMode() {
		return sslServerMode;
	}
	public void setSslServerMode(SSLServerMode sslServerMode) {
		this.sslServerMode = sslServerMode;
	}
	
	@EnvironmentSpecific
	public Integer getIoPoolSize() {
		return ioPoolSize;
	}
	public void setIoPoolSize(Integer ioPoolSize) {
		this.ioPoolSize = ioPoolSize;
	}
	
	@EnvironmentSpecific
	public Integer getMaxTotalConnections() {
		return maxTotalConnections;
	}
	public void setMaxTotalConnections(Integer maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}
	
	@EnvironmentSpecific
	public Integer getMaxConnectionsPerClient() {
		return maxConnectionsPerClient;
	}
	public void setMaxConnectionsPerClient(Integer maxConnectionsPerClient) {
		this.maxConnectionsPerClient = maxConnectionsPerClient;
	}
	
	public Long getMaxSizePerRequest() {
		return maxSizePerRequest;
	}
	public void setMaxSizePerRequest(Long maxSizePerRequest) {
		this.maxSizePerRequest = maxSizePerRequest;
	}
	
	@EnvironmentSpecific
	public Long getReadTimeout() {
		return readTimeout;
	}
	public void setReadTimeout(Long readTimeout) {
		this.readTimeout = readTimeout;
	}
	
	@EnvironmentSpecific
	public Long getWriteTimeout() {
		return writeTimeout;
	}
	public void setWriteTimeout(Long writeTimeout) {
		this.writeTimeout = writeTimeout;
	}
	
	@EnvironmentSpecific
	public Integer getRequestLimit() {
		return requestLimit;
	}
	public void setRequestLimit(Integer requestLimit) {
		this.requestLimit = requestLimit;
	}
	
	@EnvironmentSpecific
	public Integer getResponseLimit() {
		return responseLimit;
	}
	public void setResponseLimit(Integer responseLimit) {
		this.responseLimit = responseLimit;
	}
	public Long getIdleTimeout() {
		return idleTimeout;
	}
	public void setIdleTimeout(Long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
	public Long getLifetime() {
		return lifetime;
	}
	public void setLifetime(Long lifetime) {
		this.lifetime = lifetime;
	}
}
