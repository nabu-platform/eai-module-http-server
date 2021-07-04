package be.nabu.eai.module.http.virtual;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class VirtualHostGUIManager extends BaseJAXBGUIManager<VirtualHostConfiguration, VirtualHostArtifact> {

	public VirtualHostGUIManager() {
		super("Virtual Host", VirtualHostArtifact.class, new VirtualHostManager(), VirtualHostConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected VirtualHostArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		VirtualHostArtifact virtualHostArtifact = new VirtualHostArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
		// for new ones, it is automatically enabled
		virtualHostArtifact.getConfig().setEnableCompression(true);
		return virtualHostArtifact;
	}

	@Override
	public String getCategory() {
		return "Web";
	}

}
