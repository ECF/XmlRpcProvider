/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.XmlRpcRequestConfig;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactory;
import org.apache.xmlrpc.common.XmlRpcInvocationException;
import org.apache.xmlrpc.common.XmlRpcNotAuthorizedException;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.metadata.Util;
import org.apache.xmlrpc.parser.XmlRpcRequestParser;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;
import org.apache.xmlrpc.server.ReflectiveXmlRpcHandler;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping.AuthenticationHandler;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestProcessorFactory;
import org.apache.xmlrpc.util.SAXParsers;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.eclipse.ecf.provider.xmlrpc.XmlRpcConstants;
import org.eclipse.ecf.provider.xmlrpc.XmlRpcTypeFactory;
import org.eclipse.ecf.provider.xmlrpc.identity.XmlRpcNamespace;
import org.eclipse.ecf.remoteservice.AbstractRSAContainer;
import org.eclipse.ecf.remoteservice.Constants;
import org.eclipse.ecf.remoteservice.RSARemoteServiceContainerAdapter.RSARemoteServiceRegistration;
import org.eclipse.ecf.remoteservice.asyncproxy.AsyncReturnUtil;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XmlRpcHostContainer extends AbstractRSAContainer {

	private static final long HTTPSERVICE_TIMEOUT = Long.valueOf(System.getProperty("org.eclipse.ecf.provider.xmlrpc.httpservice.timeout","30000")).longValue();

	public class HostContainerHandler implements XmlRpcHandler {

		private class MethodData {
			final Method method;
			final TypeConverter[] typeConverters;

			MethodData(Method pMethod, TypeConverterFactory pTypeConverterFactory) {
				method = pMethod;
				@SuppressWarnings("rawtypes")
				Class[] paramClasses = method.getParameterTypes();
				typeConverters = new TypeConverter[paramClasses.length];
				for (int i = 0; i < paramClasses.length; i++) {
					typeConverters[i] = pTypeConverterFactory.getTypeConverter(paramClasses[i]);
				}
			}
		}

		private final AbstractReflectiveHandlerMapping mapping;
		private final MethodData[] methods;
		@SuppressWarnings("rawtypes")
		private final Class clazz;
		private final RequestProcessorFactory requestProcessorFactory;

		public HostContainerHandler(AbstractReflectiveHandlerMapping pMapping,
				TypeConverterFactory pTypeConverterFactory, Class<?> pClass, RequestProcessorFactory pFactory,
				Method[] pMethods, String[][] pSignatures, String pMethodHelp) {
			mapping = pMapping;
			clazz = pClass;
			methods = new MethodData[pMethods.length];
			requestProcessorFactory = pFactory;
			for (int i = 0; i < methods.length; i++) {
				methods[i] = new MethodData(pMethods[i], pTypeConverterFactory);
			}
		}

		private RSARemoteServiceRegistration getRegistration(XmlRpcRequest pRequest) throws XmlRpcException {
			return (RSARemoteServiceRegistration) requestProcessorFactory.getRequestProcessor(pRequest);
		}

		@Override
		public Object execute(XmlRpcRequest pRequest) throws XmlRpcException {
			AuthenticationHandler authHandler = mapping.getAuthenticationHandler();
			if (authHandler != null && !authHandler.isAuthorized(pRequest)) {
				throw new XmlRpcNotAuthorizedException("Not authorized");
			}
			Object[] args = new Object[pRequest.getParameterCount()];
			for (int j = 0; j < args.length; j++) {
				args[j] = pRequest.getParameter(j);
			}
			for (int i = 0; i < methods.length; i++) {
				MethodData methodData = methods[i];
				TypeConverter[] converters = methodData.typeConverters;
				if (args.length == converters.length) {
					boolean matching = true;
					for (int j = 0; j < args.length; j++) {
						if (!converters[j].isConvertable(args[j])) {
							matching = false;
							break;
						}
					}
					if (matching) {
						for (int j = 0; j < args.length; j++) {
							args[j] = converters[j].convert(args[j]);
						}
						return invoke(getRegistration(pRequest), methodData.method, args,
								((RemoteServiceXmlRpcRequest) pRequest).getTimeout());
					}
				}
			}
			throw new XmlRpcException("No method matching arguments: " + Util.getSignature(args));
		}

		private Object invokeWithTimeout(RSARemoteServiceRegistration reg, Method pMethod, Object[] pArgs, long timeout)
				throws XmlRpcException {
			ExecutorService es = Executors.newCachedThreadPool();
			Future<Object> f = es.submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					try {
						return pMethod.invoke(reg.getService(), pArgs);
					} catch (IllegalAccessException e) {
						throw new XmlRpcException(
								"Illegal access to method " + pMethod.getName() + " in class " + clazz.getName(), e);
					} catch (IllegalArgumentException e) {
						throw new XmlRpcException(
								"Illegal argument for method " + pMethod.getName() + " in class " + clazz.getName(), e);
					} catch (InvocationTargetException e) {
						Throwable t = e.getTargetException();
						if (t instanceof XmlRpcException)
							throw (XmlRpcException) t;
						throw new XmlRpcInvocationException("Failed to invoke method " + pMethod.getName()
								+ " in class " + clazz.getName() + ": " + t.getMessage(), t);
					}
				}
			});
			try {
				return (timeout == 0) ? f.get() : f.get(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				throw new XmlRpcException(
						"Interrupted access to method " + pMethod.getName() + " in class " + clazz.getName(), e);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof XmlRpcException)
					throw (XmlRpcException) cause;
				throw new XmlRpcException(
						"Unexplained exception for method " + pMethod.getName() + " in class " + clazz.getName(),
						cause);
			} catch (TimeoutException e) {
				throw new XmlRpcException(
						"Timeout exception for method " + pMethod.getName() + " in class " + clazz.getName(), e);
			}
		}

		private Object invoke(RSARemoteServiceRegistration reg, Method method, Object[] pArgs, long timeout)
				throws XmlRpcException {
			Object result = invokeWithTimeout(reg, method, pArgs, timeout);
			if (result != null) {
				@SuppressWarnings("rawtypes")
				Class returnType = method.getReturnType();
				// provider must expose osgi.async property and must be async return type
				if (reg.getProperty(Constants.OSGI_ASYNC_INTENT) != null && AsyncReturnUtil.isAsyncType(returnType))
					try {
						return AsyncReturnUtil.convertAsyncToReturn(result, returnType, timeout);
					} catch (Exception e) {
						throw new XmlRpcException(
								"Unexplained exception for method " + method.getName() + " in class " + clazz.getName(),
								e);
					}
			}
			return result;
		}
	}

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
								throw new XmlRpcException(
										"Could not find registration for request method name=" + arg0.getMethodName());
							return reg;
						}
					};
				}
			});
		}

		@SuppressWarnings("rawtypes")
		protected XmlRpcHandler newXmlRpcHandler(Class pClass, Method[] pMethods) throws XmlRpcException {
			String[][] sig = getSignature(pMethods);
			String help = getMethodHelp(pClass, pMethods);
			RequestProcessorFactory factory = getRequestProcessorFactoryFactory().getRequestProcessorFactory(pClass);
			if (sig == null || help == null) {
				return new ReflectiveXmlRpcHandler(this, getTypeConverterFactory(), pClass, factory, pMethods);
			}
			return new HostContainerHandler(this, getTypeConverterFactory(), pClass, factory, pMethods, sig, help);
		}

		public void register(Long key, Class<?> clazz) throws XmlRpcException {
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

	public class RemoteServiceXmlRpcRequest implements XmlRpcRequest {

		private XmlRpcRequestConfig config;
		private String methodName;
		@SuppressWarnings("rawtypes")
		private List args;
		private long timeout;
		private RSARemoteServiceRegistration reg;

		public RemoteServiceXmlRpcRequest(XmlRpcRequestConfig config, String methodName,
				@SuppressWarnings("rawtypes") List args, long timeout) {
			super();
			this.config = config;
			this.methodName = methodName;
			this.args = args;
			this.timeout = timeout;
		}

		@Override
		public XmlRpcRequestConfig getConfig() {
			return config;
		}

		@Override
		public String getMethodName() {
			return methodName;
		}

		@Override
		public int getParameterCount() {
			return args.size();
		}

		@Override
		public Object getParameter(int pIndex) {
			return args.get(pIndex);
		}

		public long getTimeout() {
			return timeout;
		}

		public void setRegistration(RSARemoteServiceRegistration r) {
			this.reg = r;
		}

		public RSARemoteServiceRegistration getRegistration() {
			return this.reg;
		}
	}

	public class RemoteServiceXmlRpcServlet extends XmlRpcServlet {

		private static final long serialVersionUID = -6645840670791304079L;

		class RemoteServiceXmlRpcServletServer extends XmlRpcServletServer {

			private long timeout = 0;

			@Override
			public void execute(HttpServletRequest pRequest, HttpServletResponse pResponse)
					throws ServletException, IOException {
				String hv = pRequest.getHeader(XmlRpcConstants.OSGI_BASIC_TIMEOUT_HEADER);
				if (hv != null) {
					try {
						timeout = Long.valueOf(hv);
					} catch (NumberFormatException e) {
						// ignore if not present
					}
				}
				super.execute(pRequest, pResponse);
			}

			protected XmlRpcRequest getRequest(final XmlRpcStreamRequestConfig pConfig, InputStream pStream)
					throws XmlRpcException {
				final XmlRpcRequestParser parser = new XmlRpcRequestParser(pConfig, getTypeFactory());
				final XMLReader xr = SAXParsers.newXMLReader();
				xr.setContentHandler(parser);
				try {
					xr.parse(new InputSource(pStream));
				} catch (SAXException e) {
					Exception ex = e.getException();
					if (ex != null && ex instanceof XmlRpcException) {
						throw (XmlRpcException) ex;
					}
					throw new XmlRpcException("Failed to parse XML-RPC request: " + e.getMessage(), e);
				} catch (IOException e) {
					throw new XmlRpcException("Failed to read XML-RPC request: " + e.getMessage(), e);
				}
				return new RemoteServiceXmlRpcRequest(pConfig, parser.getMethodName(), parser.getParams(), timeout);
			}
		}

		@Override
		protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {
			return mapping;
		}

		@Override
		protected XmlRpcServletServer newXmlRpcServer(ServletConfig pConfig) throws XmlRpcException {
			XmlRpcServletServer server = new RemoteServiceXmlRpcServletServer();
			XmlRpcServerConfigImpl configImpl = new XmlRpcServerConfigImpl();
			configImpl.setEnabledForExtensions(true);
			server.setConfig(configImpl);
			server.setTypeFactory(new XmlRpcTypeFactory(server));
			return server;
		}
	}

	protected RemoteServiceXmlRpcServlet servlet;
	protected String servletPath;
	protected RemoteServiceHandlerMapping mapping;

	public XmlRpcHostContainer(String uriContext, String path) {
		super(XmlRpcNamespace.INSTANCE.createInstance(new Object[] { uriContext + path }));
		this.servletPath = path;
		this.mapping = new RemoteServiceHandlerMapping();
	}

	protected List<RSARemoteServiceRegistration> registrations = new ArrayList<RSARemoteServiceRegistration>();

	@Override
	protected Map<String, Object> exportRemoteService(RSARemoteServiceRegistration registration) {
		HttpService httpService = HttpServiceComponent.getHttpService(HTTPSERVICE_TIMEOUT);
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
				HttpService httpService = HttpServiceComponent.getHttpService(HTTPSERVICE_TIMEOUT);
				if (httpService != null) {
					try {
						httpService.unregister(servletPath);
					} catch (Exception e) {
						// ignore
					}
					this.servlet = null;
				}
			}
		}
	}

}
