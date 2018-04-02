/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.ecf.provider.xmlrpc.Activator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;;

@Component(immediate=true,enabled=true)
public class HttpServiceComponent {

	@Reference
	synchronized void bindHttpService(HttpService svc) {
		Activator.getCF().complete(svc);
	}
	
	synchronized void unbindHttpService(HttpService svc) {
	}
	
	public static HttpService getHttpService(long timeout) {
		try {
			return Activator.getDefault().getFuture().get(timeout,TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException("Cannot get httpservice after 30 seconds",e);
		}
	}
}
