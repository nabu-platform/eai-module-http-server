package be.nabu.eai.module.http.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ExceptionSummary {
	
	public static ExceptionSummary build(Exception e) {
		ExceptionSummary summary = new ExceptionSummary();
		return summary;
	}
	
	private URI type, instance;
	private String stacktrace;
	private int status;
	private String code, message, description, identifier;
	private List<String> serviceStack = new ArrayList<String>();
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<String> getServiceStack() {
		return serviceStack;
	}
	public void setServiceStack(List<String> serviceStack) {
		this.serviceStack = serviceStack;
	}
	public String getStacktrace() {
		return stacktrace;
	}
	public void setStacktrace(String stacktrace) {
		this.stacktrace = stacktrace;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public URI getType() {
		return type;
	}
	public void setType(URI type) {
		this.type = type;
	}
	public URI getInstance() {
		return instance;
	}
	public void setInstance(URI instance) {
		this.instance = instance;
	}
}
