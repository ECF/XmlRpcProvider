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
import org.eclipse.ecf.remoteservice.IAsyncRemoteServiceProxy;
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
		final AsyncCallback cb = new AsyncCallback() {
			@Override
			public void handleError(XmlRpcRequest arg0, Throwable arg1) {
				cf.completeExceptionally(arg1);
			}

			@SuppressWarnings("unchecked")
			@Override
			public void handleResult(XmlRpcRequest arg0, Object arg1) {
				cf.complete(arg1);
			}
		};
		String asyncMethod = getAsyncMethod(remoteCall);
		try {
			synchronized (this.client) { 
				this.client.executeAsync(asyncMethod, remoteCall.getParameters(), cb);
			}
		} catch (XmlRpcException e) {
			throw new ECFException("Cannot async execute remoteCall="+remoteCall);
		}
		return cf;
	}

	@Override
	protected Object invokeSync(RSARemoteCall remoteCall) throws ECFException {
		String xmlRpcMethod = getSyncMethod(remoteCall);
		try {
			synchronized (this.client) {
				return this.client.execute(xmlRpcMethod,remoteCall.getParameters());
			}
		} catch (XmlRpcException e) {
			throw new ECFException("Could not execute remoteCall="+remoteCall,e);
		}
	}

	protected String getSyncMethod(RSARemoteCall remoteCall) {
		return String.valueOf(getRegistration().getID().getContainerRelativeID())+"."+remoteCall.getReflectMethod().getName();
	}

	protected String getAsyncMethod(RSARemoteCall remoteCall) {
		String methodName = remoteCall.getMethod();
		if (methodName.endsWith(IAsyncRemoteServiceProxy.ASYNC_METHOD_SUFFIX)) 
			methodName = methodName.substring(0,methodName.indexOf(IAsyncRemoteServiceProxy.ASYNC_METHOD_SUFFIX));
		return String.valueOf(getRegistration().getID().getContainerRelativeID())+"."+methodName;
	}
}
