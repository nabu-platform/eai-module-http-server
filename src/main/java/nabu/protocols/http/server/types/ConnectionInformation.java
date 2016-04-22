package nabu.protocols.http.server.types;

import java.util.Date;

public class ConnectionInformation {
	private String remoteHost;
	private int remotePort, localPort;
	private Date created;
	public String getRemoteHost() {
		return remoteHost;
	}
	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}
	public int getRemotePort() {
		return remotePort;
	}
	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}
	public int getLocalPort() {
		return localPort;
	}
	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
}
