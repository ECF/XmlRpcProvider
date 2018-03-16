/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc;

import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;

public class XmlRpcContainerTransportFactory extends XmlRpcSun15HttpTransportFactory {

	private long timeout;
	
	public XmlRpcContainerTransportFactory(XmlRpcClient pClient, long timeout) {
		super(pClient);
		this.timeout = timeout;
	}

    @Override
    public XmlRpcTransport getTransport() {
    	XmlRpcSun15HttpTransport transport = new XmlRpcSun15HttpTransport(getClient()) {
            @Override
            protected void initHttpHeaders(XmlRpcRequest pRequest) throws XmlRpcClientException {
                super.initHttpHeaders(pRequest);
                setRequestHeader(XmlRpcConstants.OSGI_BASIC_TIMEOUT_HEADER,String.valueOf(timeout));
            }
        };
        transport.setSSLSocketFactory(getSSLSocketFactory());
        return transport;
    }

}
