package be.nabu.eai.module.http.virtual.api;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.http.api.HTTPRequest;

public interface RequestRewriter {
	@WebResult(name = "request")
	public HTTPRequest handle(@WebParam(name = "source") Source source, @WebParam(name = "request") HTTPRequest request);
}
