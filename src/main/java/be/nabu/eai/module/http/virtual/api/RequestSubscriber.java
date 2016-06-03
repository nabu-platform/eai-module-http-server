package be.nabu.eai.module.http.virtual.api;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;

public interface RequestSubscriber {
	@WebResult(name = "response")
	public HTTPResponse handle(@WebParam(name = "source") Source source, @WebParam(name = "request") HTTPRequest request);
}
