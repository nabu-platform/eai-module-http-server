package nabu.protocols.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.HTTPProcessorFactory;
import be.nabu.libs.http.server.nio.NIOHTTPServer;
import be.nabu.libs.nio.api.MessagePipeline;
import be.nabu.libs.nio.api.Pipeline;
import be.nabu.libs.nio.api.SourceContext;
import be.nabu.libs.nio.impl.MessagePipelineImpl;
import be.nabu.libs.services.api.ExecutionContext;
import nabu.protocols.http.server.types.HttpConnectionInformation;
import nabu.protocols.http.server.types.HttpRequestSummary;
import nabu.protocols.http.server.types.HttpResponseSummary;

@WebService
public class Services {
	
	private ExecutionContext executionContext;
	
	@SuppressWarnings("unchecked")
	public List<HttpConnectionInformation> connections(@NotNull @WebParam(name = "serverId") String serverId) {
		HTTPServerArtifact resolved = executionContext.getServiceContext().getResolver(HTTPServerArtifact.class).resolve(serverId);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find server: " + serverId);
		}
		List<HttpConnectionInformation> connections = new ArrayList<HttpConnectionInformation>();
		for (Pipeline pipeline : ((NIOHTTPServer) resolved.getServer()).getPipelines()) {
			SourceContext sourceContext = pipeline.getSourceContext();
			InetSocketAddress remoteSocketAddress = ((InetSocketAddress) sourceContext.getSocketAddress());
			HttpConnectionInformation connection = new HttpConnectionInformation();
			connection.setCreated(sourceContext.getCreated());
			connection.setRemoteHost(remoteSocketAddress.getHostString());
			connection.setRemotePort(remoteSocketAddress.getPort());
			connection.setLocalPort(sourceContext.getLocalPort());
			if (pipeline instanceof MessagePipelineImpl) {
				if (((MessagePipelineImpl<?, ?>) pipeline).getMessageProcessorFactory() instanceof HTTPProcessorFactory) {
					List<HttpRequestSummary> requests = new ArrayList<HttpRequestSummary>();
					for (HTTPRequest request : (((MessagePipeline<HTTPRequest, HTTPResponse>) pipeline).getRequestQueue())) {
						requests.add(HttpRequestSummary.build(request));
					}
					connection.setRequests(requests);
					List<HttpResponseSummary> responses = new ArrayList<HttpResponseSummary>();
					for (HTTPResponse response : (((MessagePipeline<HTTPRequest, HTTPResponse>) pipeline).getResponseQueue())) {
						responses.add(HttpResponseSummary.build(response));
					}
					connection.setResponses(responses);
				}
			}
			connections.add(connection);
		}
		return connections;
	}
	
	public void disconnect(@NotNull @WebParam(name = "serverId") String serverId, @WebParam(name = "host") String host, @WebParam(name = "port") Integer port) {
		HTTPServerArtifact resolved = executionContext.getServiceContext().getResolver(HTTPServerArtifact.class).resolve(serverId);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find server: " + serverId);
		}
		List<Pipeline> pipelinesToDisconnect = new ArrayList<Pipeline>();
		for (Pipeline pipeline : ((NIOHTTPServer) resolved.getServer()).getPipelines()) {
			if (host == null) {
				pipelinesToDisconnect.add(pipeline);
			}
			else {
				SourceContext sourceContext = pipeline.getSourceContext();
				InetSocketAddress remoteSocketAddress = ((InetSocketAddress) sourceContext.getSocketAddress());
				if (host.equals(remoteSocketAddress.getHostString()) && (port == null || port.equals(remoteSocketAddress.getPort()))) {
					pipelinesToDisconnect.add(pipeline);
				}
			}
		}
		for (Pipeline pipeline : pipelinesToDisconnect) {
			try {
				pipeline.close();
			}
			catch (IOException e) {
				// continue
			}
		}
	}
}
