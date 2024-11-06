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

package be.nabu.eai.module.http.redirect;

import java.io.IOException;
import java.net.URI;

import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class RedirectArtifact extends JAXBArtifact<RedirectConfiguration> implements StartableArtifact, StoppableArtifact {

	private EventSubscription<HTTPRequest, HTTPResponse> subscription;

	public RedirectArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "redirect.xml", RedirectConfiguration.class);
	}

	@Override
	public void stop() throws IOException {
		subscription.unsubscribe();
		subscription = null;
	}

	@Override
	public void start() throws IOException {
		if (subscription == null && getConfig().getFromHost() != null && getConfig().isEnabled()) {
			subscription = getConfig().getFromHost().getDispatcher().subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
				@Override
				public HTTPResponse handle(HTTPRequest request) {
					try {
						boolean fromSecure = getConfig().getFromHost().getConfig().getKeyAlias() != null && getConfig().getFromHost().getServer() != null
							&& getConfig().getFromHost().getServer().isSecuring();
						URI uri = HTTPUtils.getURI(request, fromSecure);
						if (getConfig().getFromPath() == null || uri.getPath().matches(getConfig().getFromPath())) {
							if (getConfig().getToPath() != null) {
								uri = uri.resolve(getConfig().getToPath());
							}
							VirtualHostArtifact toHost = getConfig().getToHost() == null ? getConfig().getFromHost() : getConfig().getToHost();
							boolean toSecure = toHost.getConfig().getKeyAlias() != null && toHost.getServer() != null
								&& toHost.getServer().isSecuring();
							if (toHost.getConfig().getHost() != null && (!toHost.getConfig().getHost().equals(uri.getHost()) || fromSecure != toSecure)) {
								String authority = null;
								if (uri.getHost().equals(toHost.getConfig().getHost())) {
									authority = uri.getHost();
								}
								// allow redirects to use aliases
								else if (toHost.getConfig().getAliases() != null && toHost.getConfig().getAliases().contains(uri.getHost())) {
									authority = uri.getHost();
								}
								// we want to allow automatic redirects from "www.example.com" to "example.com" and from "example.com" to "www.example.com" depending on the settings
								else if (toHost.getConfig().getAliases() != null) {
									if (uri.getHost().startsWith("www.")) {
										String hostToTry = uri.getHost().substring("www.".length());
										if (toHost.getConfig().getAliases().contains(hostToTry)) {
											authority = hostToTry;
										}
									}
									else if (toHost.getConfig().getAliases().contains("www." + uri.getHost())) {
										authority = "www." + uri.getHost();
									}
								}
								if (authority == null) {
									authority = toHost.getConfig().getHost();
								}
								if (authority == null) {
									throw new HTTPException(500, "No server host configured for redirect in: " + getConfig().getToHost().getId());
								}
								if (getConfig().getToHost().getServer() != null) {
									Integer port = getConfig().getToHost().getServer().getConfig().getPort();
									if (port != null) {
										authority += ":" + port;
									}
								}
								uri = new URI(toSecure ? "https" : "http", authority, uri.getPath(), uri.getQuery(), uri.getFragment());
							}
							int code = getConfig().isPermanent() ? 301 : 302;
							return new DefaultHTTPResponse(request, 
								code, 
								HTTPCodes.getMessage(code), 
								new PlainMimeEmptyPart(null, new MimeHeader("Content-Length", "0"),
										new MimeHeader("Location", uri.toString())));
						}
					}
					catch (Exception e) {
						throw new HTTPException(500, e);
					}
					return null;
				}
			});
		}
	}

	@Override
	public boolean isStarted() {
		return subscription != null;
	}

}
