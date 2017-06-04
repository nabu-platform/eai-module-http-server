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
		if (subscription == null && getConfig().getFromHost() != null) {
			subscription = getConfig().getFromHost().getDispatcher().subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
				@Override
				public HTTPResponse handle(HTTPRequest request) {
					try {
						boolean fromSecure = getConfig().getFromHost().getConfig().getKeyAlias() != null && getConfig().getFromHost().getConfig().getServer() != null
							&& getConfig().getFromHost().getConfig().getServer().getConfig().getKeystore() != null;
						URI uri = HTTPUtils.getURI(request, fromSecure);
						if (getConfig().getFromPath() == null || uri.getPath().matches(getConfig().getFromPath())) {
							if (getConfig().getToPath() != null) {
								uri = uri.resolve(getConfig().getToPath());
							}
							VirtualHostArtifact toHost = getConfig().getToHost() == null ? getConfig().getFromHost() : getConfig().getToHost();
							boolean toSecure = toHost.getConfig().getKeyAlias() != null && toHost.getConfig().getServer() != null
								&& toHost.getConfig().getServer().getConfig().getKeystore() != null;
							if (toHost.getConfig().getHost() != null && (!toHost.getConfig().getHost().equals(uri.getHost()) || fromSecure != toSecure)) {
								String authority = toHost.getConfig().getHost();
								if (authority == null) {
									throw new HTTPException(500, "No server host configured for redirect in: " + getConfig().getToHost().getId());
								}
								if (getConfig().getToHost().getConfig().getServer() != null) {
									Integer port = getConfig().getToHost().getConfig().getServer().getConfig().getPort();
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
