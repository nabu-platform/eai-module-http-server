package be.nabu.eai.module.http.virtual;

import java.io.IOException;

import be.nabu.eai.module.http.virtual.api.RequestRewriter;
import be.nabu.eai.module.http.virtual.api.RequestSubscriber;
import be.nabu.eai.module.http.virtual.api.ResponseRewriter;
import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.SourceContext;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.pojo.POJOUtils;

public class VirtualHostArtifact extends JAXBArtifact<VirtualHostConfiguration> implements StartableArtifact, StoppableArtifact {

	private EventDispatcher dispatcher;
	private boolean started;
	
	public VirtualHostArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "virtual-host.xml", VirtualHostConfiguration.class);
	}

	public EventDispatcher getDispatcher() {
		if (dispatcher == null) {
			synchronized(this) {
				if (dispatcher == null) {
					EventDispatcher dispatcher = new EventDispatcherImpl();
					try {
						// allow request rewriting
						if (getConfiguration().getRequestRewriter() != null) {
							final RequestRewriter requestRewriter = POJOUtils.newProxy(RequestRewriter.class, getRepository(), SystemPrincipal.ROOT, getConfiguration().getRequestRewriter());
							dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPRequest>() {
								@Override
								public HTTPRequest handle(HTTPRequest event) {
									SourceContext sourceContext = PipelineUtils.getPipeline().getSourceContext();
									return requestRewriter.handle(new SourceImpl(sourceContext), event);
								}
							});
						}
						// allow request handlers
						if (getConfiguration().getRequestSubscriber() != null) {
							final RequestSubscriber requestSubscriber = POJOUtils.newProxy(RequestSubscriber.class, getRepository(), SystemPrincipal.ROOT, getConfiguration().getRequestSubscriber());
							dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
								@Override
								public HTTPResponse handle(HTTPRequest event) {
									SourceContext sourceContext = PipelineUtils.getPipeline().getSourceContext();
									return requestSubscriber.handle(new SourceImpl(sourceContext), event);
								}
							});
						}
						if (getConfiguration().getResponseRewriter() != null) {
							final ResponseRewriter responseSubscriber = POJOUtils.newProxy(ResponseRewriter.class, getRepository(), SystemPrincipal.ROOT, getConfiguration().getResponseRewriter());
							dispatcher.subscribe(HTTPResponse.class, new EventHandler<HTTPResponse, HTTPResponse>() {
								@Override
								public HTTPResponse handle(HTTPResponse event) {
									SourceContext sourceContext = PipelineUtils.getPipeline().getSourceContext();
									return responseSubscriber.handle(new SourceImpl(sourceContext), event);
								}
							});
						}
						this.dispatcher = dispatcher;
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return dispatcher;
	}
	
	@Override
	public void stop() throws IOException {
		if (started && getConfiguration().getServer() != null) {
			if (getConfiguration().getHost() != null) {
				getConfiguration().getServer().getServer().unroute(getConfiguration().getHost());
				if (getConfiguration().getAliases() != null) {
					for (String host : getConfiguration().getAliases()) {
						getConfiguration().getServer().getServer().unroute(host);
					}
				}
			}
			else {
				getConfiguration().getServer().getServer().unroute(null);
			}
		}
		started = false;
	}

	@Override
	public void start() throws IOException {
		if (getConfiguration().getServer() != null) {
			if (getConfiguration().getHost() != null) {
				getConfiguration().getServer().getServer().route(getConfiguration().getHost(), getDispatcher());
				if (getConfiguration().getAliases() != null) {
					for (String host : getConfiguration().getAliases()) {
						getConfiguration().getServer().getServer().route(host, getDispatcher());
					}
				}
			}
			else {
				getConfiguration().getServer().getServer().route(null, getDispatcher());
			}
			started = true;
		}
	}

	@Override
	public boolean isStarted() {
		return started;
	}

}
