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

package nabu.protocols.http.server.types;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.utils.mime.api.Header;

@XmlRootElement
@XmlType(propOrder = { "method", "target", "version", "created", "headers" })
public class HttpRequestSummary {
	public static HttpRequestSummary build(HTTPRequest request) {
		HttpRequestSummary summary = new HttpRequestSummary();
		summary.setMethod(request.getMethod());
		summary.setTarget(request.getTarget());
		summary.setVersion(request.getVersion());
		if (request instanceof DefaultHTTPRequest) {
			summary.setCreated(((DefaultHTTPRequest) request).getCreated());
		}
		if (request.getContent() != null) {
			List<HeaderSummary> headers = new ArrayList<HeaderSummary>();
			for (Header header : request.getContent().getHeaders()) {
				headers.add(HeaderSummary.build(header));
			}
			summary.setHeaders(headers);
		}
		return summary;
	}
	private String method, target;
	private double version;
	private List<HeaderSummary> headers;
	private Date created;
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
}
