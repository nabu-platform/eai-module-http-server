package be.nabu.eai.module.http.virtual.api;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.http.api.HTTPResponse;

public interface ResponseRewriter {
	@WebResult(name = "response")
	public HTTPResponse handle(@WebParam(name = "source") Source source, @WebParam(name = "response") HTTPResponse response);
}
