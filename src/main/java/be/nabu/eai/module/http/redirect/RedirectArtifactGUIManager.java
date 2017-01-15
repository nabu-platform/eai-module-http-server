package be.nabu.eai.module.http.redirect;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class RedirectArtifactGUIManager extends BaseJAXBGUIManager<RedirectConfiguration, RedirectArtifact> {

	public RedirectArtifactGUIManager() {
		super("Redirect", RedirectArtifact.class, new RedirectArtifactManager(), RedirectConfiguration.class);
	}

	public String getCategory() {
		return "Web";
	}
	
	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected RedirectArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new RedirectArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

}
