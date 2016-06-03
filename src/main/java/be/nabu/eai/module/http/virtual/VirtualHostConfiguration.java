package be.nabu.eai.module.http.virtual;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "virtualHost")
@XmlType(propOrder = { "host", "aliases", "server", "keyAlias", "requestRewriter", "requestSubscriber", "responseRewriter" })
public class VirtualHostConfiguration {
	private String host;
	private String keyAlias;
	private List<String> aliases;
	private HTTPServerArtifact server;
	private DefinedService requestSubscriber, responseRewriter, requestRewriter;
	
	@EnvironmentSpecific
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	@EnvironmentSpecific
	public List<String> getAliases() {
		return aliases;
	}
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	
	@EnvironmentSpecific
	public String getKeyAlias() {
		return keyAlias;
	}
	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public HTTPServerArtifact getServer() {
		return server;
	}
	public void setServer(HTTPServerArtifact server) {
		this.server = server;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.http.virtual.api.RequestSubscriber.handle")	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRequestSubscriber() {
		return requestSubscriber;
	}
	public void setRequestSubscriber(DefinedService requestSubscriber) {
		this.requestSubscriber = requestSubscriber;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.http.virtual.api.ResponseRewriter.handle")	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getResponseRewriter() {
		return responseRewriter;
	}
	public void setResponseRewriter(DefinedService responseRewriter) {
		this.responseRewriter = responseRewriter;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.http.virtual.api.RequestRewriter.handle")	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRequestRewriter() {
		return requestRewriter;
	}
	public void setRequestRewriter(DefinedService requestRewriter) {
		this.requestRewriter = requestRewriter;
	}
}
