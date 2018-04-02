/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.parser.ByteArrayParser;
import org.eclipse.ecf.core.util.OSGIObjectInputStream;

public class XmlRpcSerializableParser extends ByteArrayParser {
	public Object getResult() throws XmlRpcException {
		try {
			byte[] res = (byte[]) super.getResult();
			ByteArrayInputStream bais = new ByteArrayInputStream(res);
			OSGIObjectInputStream ois = new OSGIObjectInputStream(Activator.getDefault().getContext().getBundle(),bais);
			Object result = ois.readObject();
			ois.close();
			return result;
		} catch (IOException e) {
			throw new XmlRpcException("Failed to read result object: " + e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			throw new XmlRpcException("Failed to load class for result object: " + e.getMessage(), e);
		}
	}
}

