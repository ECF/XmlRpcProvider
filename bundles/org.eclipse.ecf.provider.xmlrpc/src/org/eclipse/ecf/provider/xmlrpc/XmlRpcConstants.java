/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc;

public interface XmlRpcConstants {

	String NAMESPACE_NAME = "ecf.namespace.xmlrpc";
	String SERVER_PROVIDER_CONFIG_TYPE = "ecf.xmlrpc.server";
	String CLIENT_PROVIDER_CONFIG_TYPE = "ecf.xmlrpc.client";
	String SERVER_SVCPROP_PATH = "uriPath";
	String SERVER_DEFAULT_PATH = "/xml-rpc";
	String SERVER_SVCPROP_URICONTEXT = "uriContext";
	String SERVER_DEFAULT_URICONTEXT = "http://localhost:8181";
	
}
