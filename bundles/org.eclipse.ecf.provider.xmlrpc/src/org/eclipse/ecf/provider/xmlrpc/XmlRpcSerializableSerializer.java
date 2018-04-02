/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.ws.commons.util.Base64;
import org.apache.ws.commons.util.Base64.Encoder;
import org.apache.ws.commons.util.Base64.EncoderOutputStream;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.eclipse.ecf.core.util.OSGIObjectOutputStream;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class XmlRpcSerializableSerializer extends TypeSerializerImpl {

	private String tag;
	private String exTag;

	public XmlRpcSerializableSerializer(String tag) {
		this.tag = tag;
		this.exTag = "ex:" + tag;
	}
	
	public XmlRpcSerializableSerializer() {
		this("serializable");
	}
	
	public void write(final ContentHandler pHandler, Object pObject) throws SAXException {
		pHandler.startElement("", VALUE_TAG, VALUE_TAG, ZERO_ATTRIBUTES);
		pHandler.startElement("", tag, exTag, ZERO_ATTRIBUTES);
		char[] buffer = new char[1024];
		Encoder encoder = new Base64.SAXEncoder(buffer, 0, null, pHandler);
		try {
			OutputStream ostream = new EncoderOutputStream(encoder);
			ObjectOutputStream oos = new OSGIObjectOutputStream(ostream,true);
			oos.writeObject(pObject);
			oos.close();
		} catch (Base64.SAXIOException e) {
			throw e.getSAXException();
		} catch (IOException e) {
			throw new SAXException(e);
		}
		pHandler.endElement("", tag, exTag);
		pHandler.endElement("", VALUE_TAG, VALUE_TAG);
	}

}
