/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.eclipse.ecf.provider.xmlrpc.identity.XmlRpcNamespace;
import org.eclipse.ecf.remoteservice.AbstractRSAContainer;
import org.eclipse.ecf.remoteservice.RSARemoteServiceContainerAdapter.RSARemoteServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class XmlRpcHostContainer extends AbstractRSAContainer {

	public class RemoteServiceHandlerMapping extends AbstractReflectiveHandlerMapping {
		public RemoteServiceHandlerMapping() {
			super();
			setRequestProcessorFactoryFactory(new RequestProcessorFactoryFactory() {
				@Override
				public RequestProcessorFactory getRequestProcessorFactory(@SuppressWarnings("rawtypes") Class arg0)
						throws XmlRpcException {
					return new RequestProcessorFactory() {
						@Override
						public Object getRequestProcessor(XmlRpcRequest arg0) throws XmlRpcException {
							RSARemoteServiceRegistration reg = findRegistration(arg0.getMethodName());
							if (reg == null)
								throw new XmlRpcException("Could not find registration for request method name=" + arg0.getMethodName());
							return reg.getService();
						}
					};
				}
			});
		}

		public void register(Long key, @SuppressWarnings("rawtypes") Class clazz) throws XmlRpcException {
			super.registerPublicMethods(String.valueOf(key), clazz);
		}
	}

	RSARemoteServiceRegistration findRegistration(String name) {
		String n = name.substring(0, name.indexOf('.'));
		synchronized (registrations) {
			for (RSARemoteServiceRegistration r : registrations) {
				Long id = r.getID().getContainerRelativeID();
				if (String.valueOf(id).equals(n))
					return r;
			}
		}
		return null;
	}

	public class RemoteServiceXmlRpcServlet extends XmlRpcServlet {

		private static final long serialVersionUID = -6645840670791304079L;

		@Override
		protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {
			return mapping;
		}

		@Override
		protected XmlRpcServletServer newXmlRpcServer(ServletConfig pConfig) throws XmlRpcException {
			XmlRpcServletServer server = super.newXmlRpcServer(pConfig);
			XmlRpcServerConfigImpl configImpl = new XmlRpcServerConfigImpl();
			configImpl.setEnabledForExtensions(true);
			server.setConfig(configImpl);
			return server;
		}
	}

	protected RemoteServiceXmlRpcServlet servlet;
	protected String servletPath;
	protected RemoteServiceHandlerMapping mapping;

	public XmlRpcHostContainer(String uriContext, String path) {
		super(XmlRpcNamespace.INSTANCE.createInstance(new Object[] { uriContext + path }));
		this.servletPath = path;
		this.mapping = createHandlerMapping();
	}

	protected RemoteServiceHandlerMapping createHandlerMapping() {
		return new RemoteServiceHandlerMapping();
	}
	
	protected List<RSARemoteServiceRegistration> registrations = new ArrayList<RSARemoteServiceRegistration>();

	@Override
	protected Map<String, Object> exportRemoteService(RSARemoteServiceRegistration registration) {
		HttpService httpService = HttpServiceComponent.getHttpService();
		if (httpService == null)
			throw new NullPointerException("Cannot export xmlrpc service as no httpService to ");
		if (servlet == null) {
			servlet = new RemoteServiceXmlRpcServlet();
			try {
				httpService.registerServlet(servletPath, servlet, null, null);
			} catch (ServletException | NamespaceException e) {
				throw new RuntimeException("Cannot register servlet");
			}
		}
		Class<?> intfClass = registration.getService().getClass().getInterfaces()[0];
		try {
			mapping.register(registration.getID().getContainerRelativeID(), intfClass);
		} catch (XmlRpcException e) {
			throw new RuntimeException("mapping exception", e);
		}
		synchronized (registrations) {
			registrations.add(registration);
		}
		return null;
	}

	@Override
	protected void unexportRemoteService(RSARemoteServiceRegistration registration) {
		synchronized (registrations) {
			registrations.remove(registration);
			if (registrations.size() == 0) {
				HttpService httpService = HttpServiceComponent.getHttpService();
				if (httpService != null) {
					httpService.unregister(servletPath);
					this.servlet = null;
				}
			}
		}
	}

}
