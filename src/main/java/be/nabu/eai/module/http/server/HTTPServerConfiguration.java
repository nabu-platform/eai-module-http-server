package be.nabu.eai.module.http.server;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.util.KeyValueMapAdapter;
import be.nabu.libs.http.core.ServerHeader;
import be.nabu.libs.types.api.annotation.Field;
import be.nabu.utils.io.SSLServerMode;

@XmlRootElement(name = "httpServer")
@XmlType(propOrder = { "enabled", "keystore", "sslServerMode", "port", "offlinePort", "poolSize", "ioPoolSize", "maxTotalConnections", "maxConnectionsPerClient", "maxSizePerRequest", "idleTimeout", "lifetime", "readTimeout", "writeTimeout", "requestLimit", "responseLimit", "maxInitialLineLength", "maxHeaderSize", "maxChunkSize", "proxied", "proxyPort", "proxySecure", "errorTypeUri", "errorInstanceUri", "headerMapping" })
public class HTTPServerConfiguration {
	private Integer port, offlinePort;
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
	private Integer proxyPort;
	private boolean proxySecure;
	private URI errorTypeUri, errorInstanceUri;
	
	private Map<String, String> headerMapping;
	
	@Comment(title = "The port that the server will be listening on")
	@EnvironmentSpecific
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	
	@Field(group = "security", comment = "You can enable ssl by configuring a keystore here and setting the alias to the correct key on each virtual host")
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getKeystore() {
		return keystore;
	}
	public void setKeystore(KeyStoreArtifact keystore) {
		this.keystore = keystore;
	}
	
	@Field(group = "limits", comment = "The amount of threads that the server has to process incoming requests, the default can be overwritten using the system property 'be.nabu.eai.http.processPoolSize'")
	@EnvironmentSpecific
	public Integer getPoolSize() {
		return poolSize;
	}
	public void setPoolSize(Integer poolSize) {
		this.poolSize = poolSize;
	}
	
	@Field(group = "security", show = "keystore != null", comment = "This option lets you decide what to do with client certificates.")
	@EnvironmentSpecific
	public SSLServerMode getSslServerMode() {
		return sslServerMode;
	}
	public void setSslServerMode(SSLServerMode sslServerMode) {
		this.sslServerMode = sslServerMode;
	}
	
	@Field(group = "limits", comment = "The amount of threads the server has to communicate with the client (both incoming and outgoing), the default can be overwritten using the system property 'be.nabu.eai.http.ioPoolSize'")
	@EnvironmentSpecific
	public Integer getIoPoolSize() {
		return ioPoolSize;
	}
	public void setIoPoolSize(Integer ioPoolSize) {
		this.ioPoolSize = ioPoolSize;
	}
	
	@Advanced
	@Field(group = "limits", comment = "The maximum amount of connections this server will accept.")
	@EnvironmentSpecific
	public Integer getMaxTotalConnections() {
		return maxTotalConnections;
	}
	public void setMaxTotalConnections(Integer maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}
	
	@Advanced
	@Field(group = "limits", comment = "The maximum amount of connections this server will accept per client.")
	@EnvironmentSpecific
	public Integer getMaxConnectionsPerClient() {
		return maxConnectionsPerClient;
	}
	public void setMaxConnectionsPerClient(Integer maxConnectionsPerClient) {
		this.maxConnectionsPerClient = maxConnectionsPerClient;
	}
	
	@Field(group = "limits", comment = "The maximum size of an incoming request in bytes, anything above this will be cut off")
	@Advanced
	public Long getMaxSizePerRequest() {
		return maxSizePerRequest;
	}
	public void setMaxSizePerRequest(Long maxSizePerRequest) {
		this.maxSizePerRequest = maxSizePerRequest;
	}
	
	@Field(group = "limits", comment = "The maximum amount of time it can take for a single request to make it to the server, measured from the arrival of the first byte.")
	@EnvironmentSpecific
	public Long getReadTimeout() {
		return readTimeout;
	}
	public void setReadTimeout(Long readTimeout) {
		this.readTimeout = readTimeout;
	}
	
	@Field(group = "limits", comment = "The maximum amount of time it can take for a single response to make it to the client, measured from the departure of the first byte.")
	@EnvironmentSpecific
	public Long getWriteTimeout() {
		return writeTimeout;
	}
	public void setWriteTimeout(Long writeTimeout) {
		this.writeTimeout = writeTimeout;
	}
	
	@Field(group = "limits", comment = "The maximum amount of requests that can be queued for processing for a given connection. The default is unlimited.")
	@EnvironmentSpecific
	public Integer getRequestLimit() {
		return requestLimit;
	}
	public void setRequestLimit(Integer requestLimit) {
		this.requestLimit = requestLimit;
	}
	
	@Field(group = "limits", comment = "The maximum amount of responses that can be queued for a given connection. The default is unlimited.")
	@EnvironmentSpecific
	public Integer getResponseLimit() {
		return responseLimit;
	}
	public void setResponseLimit(Integer responseLimit) {
		this.responseLimit = responseLimit;
	}
	
	@Field(group = "limits", comment = "How long a connection can be idle before it is disconnected by the server, the default is 5 minutes. If set to 0 it will never time out.")
	public Long getIdleTimeout() {
		return idleTimeout;
	}
	public void setIdleTimeout(Long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
	
	@Field(group = "limits", comment = "How long a connection (even an active one) can exist before it is disconnected by the server, the default is 1 hour. If set to 0 it will never be disconnected.")
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
	
	@Field(group = "limits", comment = "The maximum size of the initial line in the HTTP request")
	public Integer getMaxInitialLineLength() {
		return maxInitialLineLength;
	}
	public void setMaxInitialLineLength(Integer maxInitialLineLength) {
		this.maxInitialLineLength = maxInitialLineLength;
	}
	
	@Field(group = "limits", comment = "The maximum size of the headers in the HTTP request")
	public Integer getMaxHeaderSize() {
		return maxHeaderSize;
	}
	public void setMaxHeaderSize(Integer maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
	}
	
	@Field(group = "limits", comment = "The maximum size a single chunk in a chunked HTTP request")
	public Integer getMaxChunkSize() {
		return maxChunkSize;
	}
	public void setMaxChunkSize(Integer maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}
	
	@EnvironmentSpecific
	@Field(group = "proxy", comment = "Whether or not there is a proxy in front of this server.")
	public boolean isProxied() {
		return proxied;
	}
	public void setProxied(boolean proxied) {
		this.proxied = proxied;
	}
	
	@EnvironmentSpecific
	@Field(group = "proxy", show = "proxied", comment = "The port the proxy is on.")
	public Integer getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}
	
	@EnvironmentSpecific
	@Field(group = "proxy", show = "proxied", comment = "Whether or not the proxy is secure.")
	public boolean isProxySecure() {
		return proxySecure;
	}
	public void setProxySecure(boolean proxySecure) {
		this.proxySecure = proxySecure;
	}
	
	@EnvironmentSpecific
	@Field(group = "proxy", show = "proxied")
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getHeaderMapping() {
		if (headerMapping == null) {
			headerMapping = new HashMap<String, String>();
		}
		if (headerMapping.isEmpty()) {
			for (ServerHeader header : ServerHeader.values()) {
				headerMapping.put(header.getName(), null);
			}
		}
		return headerMapping;
	}
	public void setHeaderMapping(Map<String, String> headerMapping) {
		this.headerMapping = headerMapping;
	}
	
	@Advanced
	@Comment(title = "The uri that will be used to explain error types in structural error messages, you can use these replacements if necessary: {code}, {status}")
	public URI getErrorTypeUri() {
		return errorTypeUri;
	}
	public void setErrorTypeUri(URI errorTypeUri) {
		this.errorTypeUri = errorTypeUri;
	}
	
	@Advanced
	@Comment(title = "The uri that will be used to explain error instances in structural error messages, you can use these replacements if necessary: {code}, {status}, {identifier}")
	public URI getErrorInstanceUri() {
		return errorInstanceUri;
	}
	public void setErrorInstanceUri(URI errorInstanceUri) {
		this.errorInstanceUri = errorInstanceUri;
	}
	
	@Advanced
	public Integer getOfflinePort() {
		return offlinePort;
	}
	public void setOfflinePort(Integer offlinePort) {
		this.offlinePort = offlinePort;
	}
	
}
