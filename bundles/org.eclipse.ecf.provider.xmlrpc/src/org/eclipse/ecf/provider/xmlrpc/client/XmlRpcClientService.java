/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.client;

import java.util.concurrent.Callable;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.eclipse.ecf.remoteservice.client.AbstractClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientService;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;
import org.eclipse.ecf.remoteservice.events.IRemoteCallCompleteEvent;

public class XmlRpcClientService extends AbstractRSAClientService {

	private final XmlRpcClient client;

	public XmlRpcClientService(AbstractClientContainer container, RemoteServiceClientRegistration registration,
			XmlRpcClient client) {
		super(container, registration);
		this.client = client;
	}

	@Override
	protected Callable<IRemoteCallCompleteEvent> getAsyncCallable(final RSARemoteCall call) {
		return () -> {
			synchronized (XmlRpcClientService.this.client) {
				return createRCCESuccess(XmlRpcClientService.this.client.execute(getXmlRpcMethod(call.getMethod()),
						call.getParameters()));
			}
		};
	}

	@Override
	protected Callable<Object> getSyncCallable(final RSARemoteCall call) {
		return () -> {
			synchronized (XmlRpcClientService.this.client) {
				return XmlRpcClientService.this.client.execute(getXmlRpcMethod(call.getReflectMethod().getName()),
						call.getParameters());
			}
		};
	}

	String getXmlRpcMethod(String methodName) {
		return String.valueOf(getRegistration().getProperty(org.eclipse.ecf.remoteservice.Constants.SERVICE_ID)) + "."
				+ methodName;
	}
}
