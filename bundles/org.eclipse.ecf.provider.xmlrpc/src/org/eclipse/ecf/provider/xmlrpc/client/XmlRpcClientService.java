/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.client;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.remoteservice.client.AbstractClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientService;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;

public class XmlRpcClientService extends AbstractRSAClientService {

	private final XmlRpcClient client;
	
	public XmlRpcClientService(AbstractClientContainer container, RemoteServiceClientRegistration registration, XmlRpcClient client) {
		super(container, registration);
		this.client = client;
	}

	@Override
	protected Object invokeAsync(RSARemoteCall remoteCall) throws ECFException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object invokeSync(RSARemoteCall remoteCall) throws ECFException {
		String xmlRpcMethod = createMethod(remoteCall);
		try {
			return client.execute(xmlRpcMethod,remoteCall.getParameters());
		} catch (XmlRpcException e) {
			throw new ECFException("Could not execute remoteCall="+remoteCall,e);
		}
	}

	protected String createMethod(RSARemoteCall remoteCall) {
		return String.valueOf(getRegistration().getID().getContainerRelativeID())+"."+remoteCall.getReflectMethod().getName();
	}

}
