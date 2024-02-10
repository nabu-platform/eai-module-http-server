package be.nabu.eai.module.http.server.error;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.services.api.ServiceException;

public interface CustomExceptionFormatter {
	
	public enum WhitelistLevel {
		NONE,
		LIMITED,
		FULL
	}
	
	@WebResult(name = "response")
	public HTTPResponse format(
		@WebParam(name = "request") HTTPRequest request, 
		@WebParam(name = "rootException") HTTPException rootException, 
		@WebParam(name = "serviceException") ServiceException serviceException,
		// whether or not this exception should reveal all or hide all
		@WebParam(name = "whitelist") WhitelistLevel whitelist,
		@WebParam(name = "httpCode") int httpCode,
		@WebParam(name = "errorCode") String errorCode,
		@WebParam(name = "identifier") String identifier);

}
