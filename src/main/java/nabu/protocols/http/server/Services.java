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
import nabu.protocols.http.server.types.HTTPConnectionInformation;
import nabu.protocols.http.server.types.HTTPRequestSummary;
import nabu.protocols.http.server.types.HTTPResponseSummary;

@WebService
public class Services {
	
	private ExecutionContext executionContext;
	
	@SuppressWarnings("unchecked")
	public List<HTTPConnectionInformation> connections(@NotNull @WebParam(name = "serverId") String serverId) {
		HTTPServerArtifact resolved = executionContext.getServiceContext().getResolver(HTTPServerArtifact.class).resolve(serverId);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find server: " + serverId);
		}
		List<HTTPConnectionInformation> connections = new ArrayList<HTTPConnectionInformation>();
		for (Pipeline pipeline : ((NIOHTTPServer) resolved.getServer()).getPipelines()) {
			SourceContext sourceContext = pipeline.getSourceContext();
			InetSocketAddress remoteSocketAddress = ((InetSocketAddress) sourceContext.getSocket().getRemoteSocketAddress());
			HTTPConnectionInformation connection = new HTTPConnectionInformation();
			connection.setCreated(sourceContext.getCreated());
			connection.setRemoteHost(remoteSocketAddress.getHostString());
			connection.setRemotePort(remoteSocketAddress.getPort());
			connection.setLocalPort(sourceContext.getSocket().getLocalPort());
			if (pipeline instanceof MessagePipelineImpl) {
				if (((MessagePipelineImpl<?, ?>) pipeline).getMessageProcessorFactory() instanceof HTTPProcessorFactory) {
					List<HTTPRequestSummary> requests = new ArrayList<HTTPRequestSummary>();
					for (HTTPRequest request : (((MessagePipeline<HTTPRequest, HTTPResponse>) pipeline).getRequestQueue())) {
						requests.add(HTTPRequestSummary.build(request));
					}
					connection.setRequests(requests);
					List<HTTPResponseSummary> responses = new ArrayList<HTTPResponseSummary>();
					for (HTTPResponse response : (((MessagePipeline<HTTPRequest, HTTPResponse>) pipeline).getResponseQueue())) {
						responses.add(HTTPResponseSummary.build(response));
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
				InetSocketAddress remoteSocketAddress = ((InetSocketAddress) sourceContext.getSocket().getRemoteSocketAddress());
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
