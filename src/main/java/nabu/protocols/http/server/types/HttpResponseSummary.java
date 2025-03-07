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

import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.utils.mime.api.Header;

@XmlRootElement
@XmlType(propOrder = { "code", "message", "version", "created", "headers" })
public class HttpResponseSummary {
	public static HttpResponseSummary build(HTTPResponse response) {
		HttpResponseSummary summary = new HttpResponseSummary();
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
