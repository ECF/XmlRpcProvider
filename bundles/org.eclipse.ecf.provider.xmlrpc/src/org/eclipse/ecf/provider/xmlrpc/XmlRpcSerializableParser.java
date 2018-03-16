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
import java.io.ObjectInputStream;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.parser.ByteArrayParser;
import org.eclipse.ecf.core.util.ClassResolverObjectInputStream;

public class XmlRpcSerializableParser extends ByteArrayParser {
	public Object getResult() throws XmlRpcException {
		try {
			byte[] res = (byte[]) super.getResult();
			ByteArrayInputStream bais = new ByteArrayInputStream(res);
			ObjectInputStream ois = ClassResolverObjectInputStream.create(Activator.getDefault().getContext(),bais);
			return ois.readObject();
		} catch (IOException e) {
			throw new XmlRpcException("Failed to read result object: " + e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			throw new XmlRpcException("Failed to load class for result object: " + e.getMessage(), e);
		}
	}
}

