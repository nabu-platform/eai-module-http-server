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

package be.nabu.eai.module.http.server;

import java.net.URI;

import javax.xml.bind.annotation.XmlType;

import be.nabu.eai.module.http.server.error.StandardizedError;

@XmlType(propOrder = { "type", "instance", "status", "title", "detail" })
public class StandardizedErrorBase implements StandardizedError {
	private URI type, instance;
	private String detail, title;
	private Integer status;
	
	@Override
	public URI getType() {
		return type;
	}
	public void setType(URI type) {
		this.type = type;
	}
	
	@Override
	public URI getInstance() {
		return instance;
	}
	public void setInstance(URI instance) {
		this.instance = instance;
	}
	
	@Override
	public String getDetail() {
		return detail;
	}
	public void setDetail(String detail) {
		this.detail = detail;
	}
	
	@Override
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
}
