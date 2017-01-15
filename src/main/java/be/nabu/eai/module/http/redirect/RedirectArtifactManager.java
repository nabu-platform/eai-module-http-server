package be.nabu.eai.module.http.redirect;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class RedirectArtifactManager extends JAXBArtifactManager<RedirectConfiguration, RedirectArtifact> {

	public RedirectArtifactManager() {
		super(RedirectArtifact.class);
	}

	@Override
	protected RedirectArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new RedirectArtifact(id, container, repository);
	}

}
