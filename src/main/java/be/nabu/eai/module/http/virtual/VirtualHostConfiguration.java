package be.nabu.eai.module.http.virtual;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.Hidden;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.annotation.Field;

@XmlRootElement(name = "virtualHost")
@XmlType(propOrder = { "host", "aliases", "redirectAliases", "server", "keyAlias", "requestRewriter", "requestSubscriber", "responseRewriter", "useAcme", "enableHsts", "hstsMaxAge", "hstsPreload", "hstsSubDomains", "enableRangeSupport", "enableCompression", "internalServer", "captureErrors", "captureSuccessful", "defaultHost", "heartbeat" })
public class VirtualHostConfiguration {
	private String host;
	private String keyAlias;
	private List<String> aliases, redirectAliases;
	private HTTPServerArtifact server;
	private DefinedService requestSubscriber, responseRewriter, requestRewriter;
	private boolean useAcme;
	private boolean enableHsts, hstsPreload, hstsSubDomains, enableRangeSupport, enableCompression = true;
	private Long hstsMaxAge;
	// mount on the internal server, can be used to offer additional services that mix integration server with developer
	private boolean internalServer;
	// capture requests based on the response
	private boolean captureErrors, captureSuccessful;
	// if this is the default host, we will subscribe to all calls instead of only the host
	private boolean defaultHost;
	private boolean heartbeat = true;
	
	@Comment(title = "The host name, it is not required for the web application to work but some modules might need a valid host (e.g. for redirecting)", description = "Once you have filled in a host name, it is also filtered that only requests to that host (or its aliases) arrive at this virtual host")
	@EnvironmentSpecific
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	@Comment(title = "Aliases for the host name, this allows for additional valid host names to arrive at this virtual host")
	@EnvironmentSpecific
	public List<String> getAliases() {
		return aliases;
	}
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	
	@Comment(title = "Redirect aliases for the host name, these will be redirected to a valid host or alias")
	public List<String> getRedirectAliases() {
		return redirectAliases;
	}
	public void setRedirectAliases(List<String> redirectAliases) {
		this.redirectAliases = redirectAliases;
	}
	
	@Field(group = "security", comment = "The alias of the private key for the SSL connection in the keystore configured in the server")
	@EnvironmentSpecific
	public String getKeyAlias() {
		return keyAlias;
	}
	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}
	
	@Field(hide = "internalServer", comment = "The server this virtual host is operating on")
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public HTTPServerArtifact getServer() {
		return server;
	}
	public void setServer(HTTPServerArtifact server) {
		this.server = server;
	}
	
	@Advanced
	@Comment(title = "Allows you to subscribe to all incoming requests")
	@InterfaceFilter(implement = "be.nabu.eai.module.http.virtual.api.RequestSubscriber.handle")	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRequestSubscriber() {
		return requestSubscriber;
	}
	public void setRequestSubscriber(DefinedService requestSubscriber) {
		this.requestSubscriber = requestSubscriber;
	}
	
	@Advanced
	@Comment(title = "Allows you to rewrite responses before they are sent to the client")
	@InterfaceFilter(implement = "be.nabu.eai.module.http.virtual.api.ResponseRewriter.handle")	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getResponseRewriter() {
		return responseRewriter;
	}
	public void setResponseRewriter(DefinedService responseRewriter) {
		this.responseRewriter = responseRewriter;
	}
	
	@Advanced
	@Comment(title = "Allows you to rewrite requests before they are sent to the web application")
	@InterfaceFilter(implement = "be.nabu.eai.module.http.virtual.api.RequestRewriter.handle")	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRequestRewriter() {
		return requestRewriter;
	}
	public void setRequestRewriter(DefinedService requestRewriter) {
		this.requestRewriter = requestRewriter;
	}
	
	@Advanced
	@Comment(title = "If toggle the indicated key will not be used as certificate but instead the acme protocol will be used to automatically request a certificate")
	@EnvironmentSpecific
	@Hidden
	public boolean isUseAcme() {
		return useAcme;
	}
	public void setUseAcme(boolean useAcme) {
		this.useAcme = useAcme;
	}
	
	@Field(group = "security", comment = "This automatically sets a hsts header of a year by default, you can set other timings in the max age field.")
	public boolean isEnableHsts() {
		return enableHsts;
	}
	public void setEnableHsts(boolean enableHsts) {
		this.enableHsts = enableHsts;
	}

	@Field(group = "security", show = "enableHsts", comment = "Whether or not to add this site to the preload list (check: https://hstspreload.org/). If you set this boolean it might impact the other hsts settings for minimum compliance.")
	public boolean isHstsPreload() {
		return hstsPreload;
	}
	public void setHstsPreload(boolean hstsPreload) {
		this.hstsPreload = hstsPreload;
	}
	
	@Field(group = "security", show = "enableHsts", comment = "Set a max age that is not a year")
	public Long getHstsMaxAge() {
		return hstsMaxAge;
	}
	public void setHstsMaxAge(Long hstsMaxAge) {
		this.hstsMaxAge = hstsMaxAge;
	}

	@Field(group = "security", show = "enableHsts", comment = "Whether or not to set the subdomains directive on the hsts header")
	public boolean isHstsSubDomains() {
		return hstsSubDomains;
	}
	public void setHstsSubDomains(boolean hstsSubDomains) {
		this.hstsSubDomains = hstsSubDomains;
	}
	
	@Advanced
	@Comment(title = "By setting this to true, you enable range support for your http responses. This is mostly interesting when streaming large files.")
	public boolean isEnableRangeSupport() {
		return enableRangeSupport;
	}
	public void setEnableRangeSupport(boolean enableRangeSupport) {
		this.enableRangeSupport = enableRangeSupport;
	}
	
	@Advanced
	@Comment(title = "By setting this to true, the server will attempt to compress the responses in accordance with client capabilities")
	public boolean isEnableCompression() {
		return enableCompression;
	}
	public void setEnableCompression(boolean enableCompression) {
		this.enableCompression = enableCompression;
	}
	
	@Field(hide = "server != null")
	@Advanced
	public boolean isInternalServer() {
		return internalServer;
	}
	public void setInternalServer(boolean internalServer) {
		this.internalServer = internalServer;
	}
	
	@Advanced
	public boolean isCaptureErrors() {
		return captureErrors;
	}
	public void setCaptureErrors(boolean captureErrors) {
		this.captureErrors = captureErrors;
	}
	
	@Advanced
	public boolean isCaptureSuccessful() {
		return captureSuccessful;
	}
	public void setCaptureSuccessful(boolean captureSuccessful) {
		this.captureSuccessful = captureSuccessful;
	}
	
	@Advanced
	public boolean isDefaultHost() {
		return defaultHost;
	}
	public void setDefaultHost(boolean defaultHost) {
		this.defaultHost = defaultHost;
	}
	
	// in some very few cases (e.g. reverse proxies) you want to disable this
	@Advanced
	public boolean isHeartbeat() {
		return heartbeat;
	}
	public void setHeartbeat(boolean heartbeat) {
		this.heartbeat = heartbeat;
	}

}
