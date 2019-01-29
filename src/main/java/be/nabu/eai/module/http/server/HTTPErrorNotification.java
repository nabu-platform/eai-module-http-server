package be.nabu.eai.module.http.server;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "httpError")
@XmlType(propOrder = { "method", "target", "version", "host", "userAgent", "referer", "created", "request", "exceptionSummary" })
public class HTTPErrorNotification {
	private String method, target, userAgent, referer, host;
	private double version;
	private Date created;
	
	private ExceptionSummary exceptionSummary;
	private String request;
	public ExceptionSummary getExceptionSummary() {
		return exceptionSummary;
	}
	public void setExceptionSummary(ExceptionSummary exceptionSummary) {
		this.exceptionSummary = exceptionSummary;
	}
	public String getRequest() {
		return request;
	}
	public void setRequest(String request) {
		this.request = request;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public double getVersion() {
		return version;
	}
	public void setVersion(double version) {
		this.version = version;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	public String getReferer() {
		return referer;
	}
	public void setReferer(String referer) {
		this.referer = referer;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
}