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
					HTTPServerInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}


}
