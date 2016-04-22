package be.nabu.eai.module.http.virtual.api;

import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;

public interface RequestSubscriber extends EventHandler<HTTPRequest, HTTPResponse> {
	@Override
	public HTTPResponse handle(HTTPRequest response);
}
