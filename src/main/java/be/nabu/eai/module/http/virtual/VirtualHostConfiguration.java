package be.nabu.eai.module.http.virtual;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "virtualHost")
@XmlType(propOrder = { "host", "aliases", "server", "keyAlias", "requestRewriter", "requestSubscriber", "responseRewriter", "useAcme", "enableHsts", "hstsMaxAge", "hstsPreload", "hstsSubDomains" })
public class VirtualHostConfiguration {
	private String host;
	private String keyAlias;
	private List<String> aliases;
	private HTTPServerArtifact server;
	private DefinedService requestSubscriber, responseRewriter, requestRewriter;
	private boolean useAcme;
	private boolean enableHsts, hstsPreload, hstsSubDomains;
	private Long hstsMaxAge;
	
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
	
	@Comment(title = "The alias of the private key for the SSL connection in the keystore configured in the server")
	@EnvironmentSpecific
	public String getKeyAlias() {
		return keyAlias;
	}
	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}
	
	@Comment(title = "The server this virtual host is operating on")
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
	public boolean isUseAcme() {
		return useAcme;
	}
	public void setUseAcme(boolean useAcme) {
		this.useAcme = useAcme;
	}
	
	@Advanced
	@Comment(title = "This automatically sets a hsts header of a year by default, you can set other timings in the max age field.")
	public boolean isEnableHsts() {
		return enableHsts;
	}
	public void setEnableHsts(boolean enableHsts) {
		this.enableHsts = enableHsts;
	}
	
	@Comment(title = "Whether or not to add this site to the preload list (check: https://hstspreload.org/). If you set this boolean it might impact the other hsts settings for minimum compliance.")
	@Advanced
	public boolean isHstsPreload() {
		return hstsPreload;
	}
	public void setHstsPreload(boolean hstsPreload) {
		this.hstsPreload = hstsPreload;
	}
	
	@Comment(title = "Set a max age that is not a year")
	@Advanced
	public Long getHstsMaxAge() {
		return hstsMaxAge;
	}
	public void setHstsMaxAge(Long hstsMaxAge) {
		this.hstsMaxAge = hstsMaxAge;
	}

	@Advanced
	@Comment(title = "Whether or not to set the subdomains directive on the hsts header")
	public boolean isHstsSubDomains() {
		return hstsSubDomains;
	}
	public void setHstsSubDomains(boolean hstsSubDomains) {
		this.hstsSubDomains = hstsSubDomains;
	}
	
}
