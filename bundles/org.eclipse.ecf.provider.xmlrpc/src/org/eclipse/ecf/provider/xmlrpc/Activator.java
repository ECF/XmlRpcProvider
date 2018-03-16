/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.util.BundleClassResolver;
import org.eclipse.ecf.core.util.IClassResolver;
import org.eclipse.ecf.provider.xmlrpc.client.XmlRpcClientContainer;
import org.eclipse.ecf.provider.xmlrpc.identity.XmlRpcNamespace;
import org.eclipse.ecf.provider.xmlrpc.server.XmlRpcHostContainer;
import org.eclipse.ecf.remoteservice.Constants;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

public class Activator implements BundleActivator {

	private static CompletableFuture<HttpService> cf;
	private static Activator instance;
	private BundleContext context;
	
	public static Activator getDefault() {
		return instance;
	}

	public BundleContext getContext() {
		return context;
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
		this.context = context;
		// Setup IClassResolver service so that class resolving/loading is done by this
		// bundle's classloader
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(IClassResolver.BUNDLE_PROP_NAME, context.getBundle().getSymbolicName());
		this.context.registerService(IClassResolver.class, new BundleClassResolver(context.getBundle()), props);
		
		cf = new CompletableFuture<HttpService>();
		// Register XmlRpcNamespace
		context.registerService(Namespace.class, new XmlRpcNamespace(), null);
		// Register xmlrpc server provider
		context.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(XmlRpcConstants.SERVER_PROVIDER_CONFIG_TYPE)
						.setInstantiator(
								new RemoteServiceContainerInstantiator(XmlRpcConstants.SERVER_PROVIDER_CONFIG_TYPE,
										XmlRpcConstants.CLIENT_PROVIDER_CONFIG_TYPE) {
									@Override
									public IContainer createInstance(ContainerTypeDescription description,
											Map<String, ?> parameters) {
										return new XmlRpcHostContainer(
												getParameterValue(parameters, XmlRpcConstants.SERVER_SVCPROP_URICONTEXT,
														XmlRpcConstants.SERVER_DEFAULT_URICONTEXT),
												getParameterValue(parameters, XmlRpcConstants.SERVER_SVCPROP_PATH,
														XmlRpcConstants.SERVER_DEFAULT_PATH));
									}

									@Override
									public String[] getSupportedIntents(ContainerTypeDescription description) {
										List<String> supportedIntents = new ArrayList<String>(
												Arrays.asList(super.getSupportedIntents(description)));
										supportedIntents.add(Constants.OSGI_ASYNC_INTENT);
										return supportedIntents.toArray(new String[supportedIntents.size()]);
									}
								})
						.setServer(true).setHidden(false).build(),
				null);
		// register xmlrpc client provider
		context.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(XmlRpcConstants.CLIENT_PROVIDER_CONFIG_TYPE)
						.setInstantiator(
								new RemoteServiceContainerInstantiator(XmlRpcConstants.SERVER_PROVIDER_CONFIG_TYPE,
										XmlRpcConstants.CLIENT_PROVIDER_CONFIG_TYPE) {
									@Override
									public IContainer createInstance(ContainerTypeDescription description,
											Map<String, ?> parameters) {
										return new XmlRpcClientContainer();
									}

									@Override
									public String[] getSupportedIntents(ContainerTypeDescription description) {
										List<String> supportedIntents = new ArrayList<String>(
												Arrays.asList(super.getSupportedIntents(description)));
										supportedIntents.add(Constants.OSGI_ASYNC_INTENT);
										return supportedIntents.toArray(new String[supportedIntents.size()]);
									}

								})
						.setServer(false).setHidden(false).build(),
				null);
	}

	public CompletableFuture<HttpService> getFuture() {
		return cf;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (cf != null) {
			cf.cancel(true);
			cf = null;
		}
		instance = null;
		this.context = null;
	}

}
