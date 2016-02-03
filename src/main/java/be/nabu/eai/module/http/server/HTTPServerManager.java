package be.nabu.eai.module.http.server;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class HTTPServerManager extends JAXBArtifactManager<HTTPServerConfiguration, HTTPServerArtifact> {

	public HTTPServerManager() {
		super(HTTPServerArtifact.class);
	}

	@Override
	protected HTTPServerArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new HTTPServerArtifact(id, container, repository);
	}

}
