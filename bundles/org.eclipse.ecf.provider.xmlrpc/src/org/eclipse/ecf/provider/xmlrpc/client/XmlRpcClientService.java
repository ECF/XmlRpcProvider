/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.client;

import java.util.concurrent.CompletableFuture;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.AsyncCallback;
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
		@SuppressWarnings("rawtypes")
		final CompletableFuture cf = new CompletableFuture();
		try {
			synchronized (this.client) { 
				this.client.executeAsync(getMethod(remoteCall.getMethod()), remoteCall.getParameters(), new AsyncCallback() {
					@Override
					public void handleError(XmlRpcRequest arg0, Throwable arg1) {
						// error occurred
						cf.completeExceptionally(arg1);
					}
					@SuppressWarnings("unchecked")
					@Override
					public void handleResult(XmlRpcRequest arg0, Object arg1) {
						// successful
						cf.complete(arg1);
					}
				});
			}
		} catch (XmlRpcException e) {
			throw new ECFException("Cannot async execute remoteCall="+remoteCall);
		}
		return cf;
	}

	@Override
	protected Object invokeSync(RSARemoteCall remoteCall) throws ECFException {
		try {
			synchronized (this.client) {
				return this.client.execute(getMethod(remoteCall.getReflectMethod().getName()),remoteCall.getParameters());
			}
		} catch (XmlRpcException e) {
			throw new ECFException("Could not execute remoteCall="+remoteCall,e);
		}
	}

	protected String getMethod(String methodName) {
		return String.valueOf(getRegistration().getProperty(org.eclipse.ecf.remoteservice.Constants.SERVICE_ID))+"."+methodName;
	}
}
