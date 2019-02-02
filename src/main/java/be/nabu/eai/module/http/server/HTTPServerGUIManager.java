package be.nabu.eai.module.http.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

	@Override
	public <V> void setValue(HTTPServerArtifact instance, Property<V> property, V value) {
		// we don't want to update the properties as the map has to stay the same (reference-wise)
		if (!"headerMapping".equals(property.getName())) {
			super.setValue(instance, property, value);
		}
		// we can however merge it
		else if (value instanceof Map) {
			getConfiguration(instance).getHeaderMapping().putAll(((Map<? extends String, ? extends String>) value));
		}
	}
	
	
}
