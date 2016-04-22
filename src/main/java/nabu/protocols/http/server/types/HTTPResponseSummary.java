package nabu.protocols.http.server.types;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.utils.mime.api.Header;

@XmlRootElement
@XmlType(propOrder = { "code", "message", "version", "created", "headers" })
public class HTTPResponseSummary {
	public static HTTPResponseSummary build(HTTPResponse response) {
		HTTPResponseSummary summary = new HTTPResponseSummary();
		summary.setMessage(response.getMessage());
		summary.setCode(response.getCode());
		summary.setVersion(response.getVersion());
		if (response instanceof DefaultHTTPResponse) {
			summary.setCreated(((DefaultHTTPResponse) response).getCreated());
		}
		if (response.getContent() != null) {
			List<HeaderSummary> headers = new ArrayList<HeaderSummary>();
			for (Header header : response.getContent().getHeaders()) {
				headers.add(HeaderSummary.build(header));
			}
			summary.setHeaders(headers);
		}
		return summary;
	}
	
	private double version;
	private String message;
	private int code;
	private List<HeaderSummary> headers;
	private Date created;
	public double getVersion() {
		return version;
	}
	public void setVersion(double version) {
		this.version = version;
	}
	public List<HeaderSummary> getHeaders() {
		return headers;
	}
	public void setHeaders(List<HeaderSummary> headers) {
		this.headers = headers;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
}
