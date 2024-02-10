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
