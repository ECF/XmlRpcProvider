/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc.identity;

import org.eclipse.ecf.core.identity.URIID.URIIDNamespace;
import org.eclipse.ecf.provider.xmlrpc.XmlRpcConstants;

public class XmlRpcNamespace extends URIIDNamespace {

	private static final long serialVersionUID = 6779577121601420259L;
	public static XmlRpcNamespace INSTANCE;

	public XmlRpcNamespace() {
		super(XmlRpcConstants.NAMESPACE_NAME, "XML-RPC Namespace");
		INSTANCE = this;
	}

	public static XmlRpcNamespace getInstance() {
		return INSTANCE;
	}

	public String getScheme() {
		return "xmlrpc";
	}
}
