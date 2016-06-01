/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.server;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;;

@Component(immediate=true,enabled=true)
public class HttpServiceComponent {

	private static HttpService httpService;
	
	@Reference(policy=ReferencePolicy.DYNAMIC,cardinality=ReferenceCardinality.OPTIONAL)
	void bindHttpService(HttpService svc) {
		httpService = svc;
	}
	
	void unbindHttpService(HttpService svc) {
		httpService = null;
	}
	
	public static HttpService getHttpService() {
		return httpService;
	}
}
