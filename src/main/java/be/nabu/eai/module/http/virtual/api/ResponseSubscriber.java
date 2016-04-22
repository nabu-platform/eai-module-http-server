package be.nabu.eai.module.http.virtual.api;

import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.api.HTTPResponse;

public interface ResponseSubscriber extends EventHandler<HTTPResponse, HTTPResponse> {
	@Override
	public HTTPResponse handle(HTTPResponse response);
}
