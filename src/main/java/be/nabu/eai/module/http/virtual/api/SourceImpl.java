package be.nabu.eai.module.http.virtual.api;

import java.net.InetSocketAddress;
import java.util.Date;

import be.nabu.libs.nio.api.SourceContext;

public class SourceImpl implements Source {
	
	private int remotePort, localPort;
	private String remoteHost, remoteIp;
	private Date created;
	
	public SourceImpl() {
		// auto construct
	}
	
	public SourceImpl(SourceContext context) {
		InetSocketAddress remoteSocketAddress = ((InetSocketAddress) context.getSocketAddress());
		if (remoteSocketAddress != null) {
			this.remoteIp = remoteSocketAddress.getAddress().getHostAddress();
			this.remoteHost = remoteSocketAddress.getHostName();
			this.remotePort = remoteSocketAddress.getPort();
		}
		this.localPort = context.getLocalPort();
		this.created = context.getCreated();
	}
	
	@Override
	public int getRemotePort() {
		return remotePort;
	}
	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}
	
	@Override
	public int getLocalPort() {
		return localPort;
	}
	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}
	
	@Override
	public String getRemoteHost() {
		return remoteHost;
	}
	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}
	
	@Override
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	@Override
	public String getRemoteIp() {
		return remoteIp;
	}
	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}

}
