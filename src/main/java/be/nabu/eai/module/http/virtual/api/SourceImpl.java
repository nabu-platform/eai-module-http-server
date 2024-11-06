/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
