/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.client;

import java.net.URL;
import java.util.UUID;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.eclipse.ecf.provider.xmlrpc.XmlRpcContainerTransportFactory;
import org.eclipse.ecf.provider.xmlrpc.XmlRpcTypeFactory;
import org.eclipse.ecf.provider.xmlrpc.identity.XmlRpcNamespace;
import org.eclipse.ecf.remoteservice.Constants;
import org.eclipse.ecf.remoteservice.IRemoteCall;
import org.eclipse.ecf.remoteservice.IRemoteService;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientContainer;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;

public class XmlRpcClientContainer extends AbstractRSAClientContainer {

	protected XmlRpcClient client;

	protected class ClientContainerTransport extends XmlRpcSun15HttpTransport {

		public ClientContainerTransport(XmlRpcClient pClient) {
			super(pClient);
		}
		
	}
	public XmlRpcClientContainer() {
		super(XmlRpcNamespace.getInstance().createInstance(new Object[] { "uuid:" + UUID.randomUUID().toString() }));
	}

	@Override
	protected IRemoteService createRemoteService(final RemoteServiceClientRegistration aRegistration) {
		try {
			XmlRpcClientConfigImpl configImpl = new XmlRpcClientConfigImpl();
			configImpl.setServerURL(new URL(getConnectedID().getName()));
			configImpl.setEnabledForExtensions(true);
			// Get osgi.basic
			Object o = aRegistration.getProperty(Constants.OSGI_BASIC_TIMEOUT_INTENT);
			long ltimeout = IRemoteCall.DEFAULT_TIMEOUT;
			if (o != null) {
				if (o instanceof Number)
					ltimeout = ((Number) o).longValue();
				else if (o instanceof String)
					ltimeout = Long.valueOf((String) o);
			}
			final long timeout = ltimeout;
			client = new XmlRpcClient();
			client.setConfig(configImpl);
			// setup our transport factory with the timeout given as a custom header
	        client.setTransportFactory(new XmlRpcContainerTransportFactory(client,timeout));
	        client.setTypeFactory(new XmlRpcTypeFactory(client));
			return new XmlRpcClientService(this, aRegistration, client);
		} catch (final Exception ex) {
			// TODO: log exception
			ex.printStackTrace();
			// Return null in case of error
			return null;
		}
	}

	public void dispose() {
		client = null;
	}
}
