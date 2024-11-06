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
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.StandardConstants;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.utils.io.SSLServerMode;

/**
 * SSL is a problem for virtual hosts (https://wiki.apache.org/httpd/NameBasedSSLVHosts)
 * In a nutshell: virtual hosts work by checking the domain that was requested and routing to the appropriate application
 * However if the virtual hosts are protected by SSL, the handshake occurs way before any http request is sent and before the Host header signals which host is requested!
 * To combat this, SNI (server name indication) was added to TLS handshakes. SNI is supported in java as a client from 7 onwards and as a server from 8 onwards.
 * Most browsers have supported this extension for a number of years now so in general it should not pose a problem.
 * 
 * It is possible to set SNI matchers on the engine.getSSLParameters() but I'm not sure if that is necessary in order to get the requested SNI names in this part of the code.
 * 
 * This code is in part based on http://stackoverflow.com/questions/20807408/handling-multiple-certificates-in-nettys-ssl-handler-used-in-play-framework-1-2
 * And https://github.com/grahamedgecombe/netty-sni-example/
 * TODO: how important is it to signal the chosen webartifact to the "upper" layer? Meaning we choose the correct pipeline to perform the requests?
 * 		> it is theoretically possible to connect to a host using SNI a.com and then perform requests to b.com over that secure connection
 * 		> the only downside to this is if you are using client side certificates because it will allow you to bypass
 */
public class ArtifactAwareKeyManager extends X509ExtendedKeyManager {

	private Repository repository;
	private List<VirtualHostArtifact> virtualHosts;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private X509KeyManager parent;
	private HTTPServerArtifact server;
	private KeyStore keyStore;
	
	public ArtifactAwareKeyManager(X509KeyManager parent, Repository repository, HTTPServerArtifact server, KeyStore keyStore) {
		this.parent = parent;
		this.repository = repository;
		this.server = server;
		// we want to be able to probe the keystore to make more dynamic choices
		this.keyStore = keyStore;
	}
	
	private String getAlias(VirtualHostArtifact artifact) {
		String alias = artifact.getConfig().getKeyAlias();
		if (alias == null) {
			alias = artifact.getId();
		}
		try {
			List<String> potentials = Arrays.asList("acme2-" + alias, "acme-" + alias, alias);
			for (String potential : potentials) {
				if (keyStore.containsAlias(potential)) {
					return potential;
				}
			}
		}
		catch (Exception e) {
			logger.error("Problems resolving aliases from the keystore", e);
		}
		try {
			if (keyStore.containsAlias(alias)) {
				return alias;
			}
			else if (keyStore.containsAlias(server.getDefaultAlias())) {
				return server.getDefaultAlias();
			}
		}
		catch (Exception e) {
			logger.error("Could not determine alias", e);
		}
		return null;
	}
	
	/**
	 * This method is used by the SSLEngine instance to determine which alias to use
	 */
	@Override
	public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
		// wrapped an arraylist around the virtualhosts because we had a concurrent modification exception on the iterator.remove() below
		// that being said, why is this code here? can't we just filter the virtual hosts on the server at the time of building the virtual host list? (which is synchronized)
		// why is this separate?
		List<VirtualHostArtifact> virtualHosts = new ArrayList<VirtualHostArtifact>(getVirtualHosts());
		Iterator<VirtualHostArtifact> iterator = virtualHosts.iterator();
		while (iterator.hasNext()) {
			try {
				if (!server.equals(iterator.next().getServer())) {
					iterator.remove();
				}
			}
			catch (Exception e) {
				logger.error("Could not load web artifact", e);
				iterator.remove();
			}
		}
		// no valid webartifacts
		if (virtualHosts.isEmpty()) {
			return null;
		}
		// just one, return that
		else if (virtualHosts.size() == 1) {
			return getAlias(virtualHosts.get(0));
		}
		// multiple, choose on the basis of the SNI host name (if any)
		else {
			try {
				if (SSLServerMode.NEED_CLIENT_CERTIFICATES.equals(server.getConfiguration().getSslServerMode())) {
					logger.error("Currently SNI-based resolving on multiple web artifacts is not supported if client certificates are turned on because we can't guarantee (yet) that the correct pipeline is chosen");
					return null;
				}
				ExtendedSSLSession session = (ExtendedSSLSession) engine.getHandshakeSession();
				SNIHostName hostName = null;
				for (SNIServerName name : session.getRequestedServerNames()) {
					if (name.getType() == StandardConstants.SNI_HOST_NAME) {
						hostName = (SNIHostName) name;
						break;
					}
				}
				if (hostName == null) {
					logger.error("Multiple web artifacts on a secure connection but no SNI in the original request");
					return null;
				}
				for (VirtualHostArtifact artifact : virtualHosts) {
					try {
						List<String> hosts = new ArrayList<String>();
						if (artifact.getConfiguration().getHost() != null) {
							hosts.add(artifact.getConfiguration().getHost());
						}
						if (artifact.getConfiguration().getAliases() != null) {
							hosts.addAll(artifact.getConfiguration().getAliases());
						}
						if (artifact.getConfiguration().getRedirectAliases() != null) {
							hosts.addAll(artifact.getConfiguration().getRedirectAliases());
						}
						for (String host : hosts) {
							SNIHostName sniHostName = new SNIHostName(host);
							if (sniHostName.equals(hostName)) {
								return getAlias(artifact);
							}
						}
					}
					catch (Exception e) {
						logger.error("Could not check virtual host for SNI hostname matches", e);
					}
				}
				logger.error("Found multiple virtual hosts but none had a host that matches: " + hostName);
				return null;
			}
			catch (Exception e) {
				logger.error("Could not determine virtual host", e);
				return null;
			}
		}
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		return parent.getCertificateChain(alias);
	}
	@Override
	public PrivateKey getPrivateKey(String alias) {
		return parent.getPrivateKey(alias);
	}
	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		return parent.getServerAliases(keyType, issuers);
	}

	private List<VirtualHostArtifact> getVirtualHosts() {
		// caching the results here might represent problems with dynamic virtual hosts (see waf)
		return repository.getArtifacts(VirtualHostArtifact.class);
//		if (virtualHosts == null || EAIResourceRepository.isDevelopment()) {
//			synchronized(this) {
//				if (virtualHosts == null || EAIResourceRepository.isDevelopment()) {
//					this.virtualHosts = repository.getArtifacts(VirtualHostArtifact.class);
//				}
//			}
//		}
//		return virtualHosts;
	}

	/**
	 * Unsupported
	 */
	@Override
	public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
		throw new UnsupportedOperationException();
	}
	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		throw new UnsupportedOperationException();
	}
	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		throw new UnsupportedOperationException();
	}
	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		throw new UnsupportedOperationException();
	}

}
