package be.nabu.eai.module.http.virtual.api;

import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.api.HTTPRequest;

public interface RequestRewriter extends EventHandler<HTTPRequest, HTTPRequest> {
	@Override
	public HTTPRequest handle(HTTPRequest response);
}
