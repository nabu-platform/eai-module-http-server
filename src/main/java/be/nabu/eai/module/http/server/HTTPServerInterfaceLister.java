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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import be.nabu.eai.developer.api.InterfaceLister;
import be.nabu.eai.developer.util.InterfaceDescriptionImpl;

public class HTTPServerInterfaceLister implements InterfaceLister {
	
	private static Collection<InterfaceDescription> descriptions = null;
	
	@Override
	public Collection<InterfaceDescription> getInterfaces() {
		if (descriptions == null) {
			synchronized(HTTPServerInterfaceLister.class) {
				if (descriptions == null) {
					List<InterfaceDescription> descriptions = new ArrayList<InterfaceDescription>();
					descriptions.add(new InterfaceDescriptionImpl("Virtual Host", "Request Rewriter", "be.nabu.eai.module.http.virtual.api.RequestRewriter.handle"));
					descriptions.add(new InterfaceDescriptionImpl("Virtual Host", "Request Subscriber", "be.nabu.eai.module.http.virtual.api.RequestSubscriber.handle"));
					descriptions.add(new InterfaceDescriptionImpl("Virtual Host", "Response Rewriter", "be.nabu.eai.module.http.virtual.api.ResponseRewriter.handle"));
					descriptions.add(new InterfaceDescriptionImpl("HTTP Server", "Exception Formatter", "be.nabu.eai.module.http.server.error.CustomExceptionFormatter.format"));
					HTTPServerInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}


}
