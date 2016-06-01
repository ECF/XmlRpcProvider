/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc;

import java.util.Map;

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.provider.xmlrpc.client.XmlRpcClientContainer;
import org.eclipse.ecf.provider.xmlrpc.identity.XmlRpcNamespace;
import org.eclipse.ecf.provider.xmlrpc.server.XmlRpcHostContainer;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		// Register XmlRpcNamespace
		context.registerService(Namespace.class, new XmlRpcNamespace(), null);
		context.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(XmlRpcConstants.SERVER_PROVIDER_CONFIG_TYPE)
						.setInstantiator(new RemoteServiceContainerInstantiator(
								XmlRpcConstants.SERVER_PROVIDER_CONFIG_TYPE, XmlRpcConstants.CLIENT_PROVIDER_CONFIG_TYPE) {
							@Override
							public IContainer createInstance(ContainerTypeDescription description,
									Map<String, ?> parameters) {
								return new XmlRpcHostContainer(
										getParameterValue(parameters, XmlRpcConstants.SERVER_SVCPROP_URICONTEXT,XmlRpcConstants.SERVER_DEFAULT_URICONTEXT),
										getParameterValue(parameters, XmlRpcConstants.SERVER_SVCPROP_PATH,XmlRpcConstants.SERVER_DEFAULT_PATH)
												);
							}
						}).setServer(true).setHidden(false).build(),
				null);
		context.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(XmlRpcConstants.CLIENT_PROVIDER_CONFIG_TYPE)
						.setInstantiator(new RemoteServiceContainerInstantiator(
								XmlRpcConstants.SERVER_PROVIDER_CONFIG_TYPE, XmlRpcConstants.CLIENT_PROVIDER_CONFIG_TYPE) {
							@Override
							public IContainer createInstance(ContainerTypeDescription description,
									Map<String, ?> parameters) {
								return new XmlRpcClientContainer();
							}
						}).setServer(false).setHidden(false).build(),
				null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
