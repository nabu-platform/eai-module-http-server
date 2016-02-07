package be.nabu.eai.module.http.server;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class HTTPServerGUIManager extends BaseJAXBGUIManager<HTTPServerConfiguration, HTTPServerArtifact> {

	public HTTPServerGUIManager() {
		super("HTTP Server", HTTPServerArtifact.class, new HTTPServerManager(), HTTPServerConfiguration.class);
	}

	@Override
	public String getCategory() {
		return "Protocols";
	}
	
	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected HTTPServerArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new HTTPServerArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}
}
