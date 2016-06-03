package be.nabu.eai.module.http.virtual.api;

import java.util.Date;

public interface Source {
	public String getRemoteHost();
	public String getRemoteIp();
	public int getRemotePort();
	public int getLocalPort();
	public Date getCreated();
}
