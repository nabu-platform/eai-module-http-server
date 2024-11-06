/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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

	@SuppressWarnings("unchecked")
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

	@Override
	protected String getDefaultValue(HTTPServerArtifact instance, String property) {
		if ("ioPoolSize".equals(property)) {
			return System.getProperty(HTTPServerArtifact.HTTP_IO_POOL_SIZE, "5");
		}
		else if ("poolSize".equals(property)) {
			return System.getProperty(HTTPServerArtifact.HTTP_PROCESS_POOL_SIZE, "10");
		}
		else if ("maxTotalConnections".equals(property) || "maxSizePerRequest".equals(property) || "maxConnectionsPerClient".equals(property)
				|| "readTimeout".equals(property) || "writeTimeout".equals(property) || "requestLimit".equals(property)
				|| "responseLimit".equals(property)) {
			return "unlimited";
		}
		else if ("port".equals(property)) {
			return instance.isSecuring() ? "443" : "80";
		}
		// configured in NIOServerImpl
		else if ("idleTimeout".equals(property)) {
			return "" + (5l*60*1000);
		}
		else if ("lifetime".equals(property)) {
			return "" + (60l*1000*60);
		}
		// configure in HTTPMessageParser
		else if ("maxInitialLineLength".equals(property)) {
			return "4096";
		}
		// configure in HTTPMessageParser
		else if ("maxHeaderSize".equals(property)) {
			return "8192";
		}
		// configure in HTTPMessageParser
		else if ("maxChunkSize".equals(property)) {
			return "81920";
		}
		return super.getDefaultValue(instance, property);
	}
	
	
}
