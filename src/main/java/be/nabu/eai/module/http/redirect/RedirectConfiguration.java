package be.nabu.eai.module.http.redirect;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

@XmlRootElement(name = "redirect")
@XmlType(propOrder = { "fromHost", "toHost", "fromPath", "toPath", "permanent" })
public class RedirectConfiguration {
	
	private VirtualHostArtifact fromHost, toHost;
	private String fromPath, toPath;
	private boolean permanent;
	
	@NotNull
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public VirtualHostArtifact getFromHost() {
		return fromHost;
	}
	public void setFromHost(VirtualHostArtifact fromHost) {
		this.fromHost = fromHost;
	}
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public VirtualHostArtifact getToHost() {
		return toHost;
	}
	public void setToHost(VirtualHostArtifact toHost) {
		this.toHost = toHost;
	}
	public String getToPath() {
		return toPath;
	}
	public void setToPath(String toPath) {
		this.toPath = toPath;
	}
	public String getFromPath() {
		return fromPath;
	}
	public void setFromPath(String fromPath) {
		this.fromPath = fromPath;
	}
	public boolean isPermanent() {
		return permanent;
	}
	public void setPermanent(boolean permanent) {
		this.permanent = permanent;
	}
}
