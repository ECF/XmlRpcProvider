/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmlrpc;

import java.io.Serializable;
import java.util.TimeZone;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcExtensionException;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.BigDecimalParser;
import org.apache.xmlrpc.parser.BigIntegerParser;
import org.apache.xmlrpc.parser.BooleanParser;
import org.apache.xmlrpc.parser.ByteArrayParser;
import org.apache.xmlrpc.parser.CalendarParser;
import org.apache.xmlrpc.parser.DateParser;
import org.apache.xmlrpc.parser.DoubleParser;
import org.apache.xmlrpc.parser.FloatParser;
import org.apache.xmlrpc.parser.I1Parser;
import org.apache.xmlrpc.parser.I2Parser;
import org.apache.xmlrpc.parser.I4Parser;
import org.apache.xmlrpc.parser.I8Parser;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.NodeParser;
import org.apache.xmlrpc.parser.NullParser;
import org.apache.xmlrpc.parser.ObjectArrayParser;
import org.apache.xmlrpc.parser.StringParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.BigDecimalSerializer;
import org.apache.xmlrpc.serializer.BigIntegerSerializer;
import org.apache.xmlrpc.serializer.BooleanSerializer;
import org.apache.xmlrpc.serializer.ByteArraySerializer;
import org.apache.xmlrpc.serializer.CalendarSerializer;
import org.apache.xmlrpc.serializer.DateSerializer;
import org.apache.xmlrpc.serializer.DoubleSerializer;
import org.apache.xmlrpc.serializer.FloatSerializer;
import org.apache.xmlrpc.serializer.I1Serializer;
import org.apache.xmlrpc.serializer.I2Serializer;
import org.apache.xmlrpc.serializer.I4Serializer;
import org.apache.xmlrpc.serializer.I8Serializer;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.NodeSerializer;
import org.apache.xmlrpc.serializer.NullSerializer;
import org.apache.xmlrpc.serializer.ObjectArraySerializer;
import org.apache.xmlrpc.serializer.SerializableSerializer;
import org.apache.xmlrpc.serializer.StringSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.XmlRpcWriter;
import org.apache.xmlrpc.util.XmlRpcDateTimeDateFormat;
import org.osgi.dto.DTO;
import org.osgi.framework.Version;
import org.xml.sax.SAXException;

public class XmlRpcTypeFactory extends TypeFactoryImpl {

	public XmlRpcTypeFactory(XmlRpcController pController) {
		super(pController);
	}
	@Override
	public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
		TypeSerializer superSerializer = super.getSerializer(pConfig, pObject);
		if (superSerializer != null)
			return superSerializer;
		if (pObject instanceof Serializable) {
			if (pConfig.isEnabledForExtensions()) {
				return new XmlRpcSerializableSerializer();
			} else {
				throw new SAXException(new XmlRpcExtensionException("Serializable objects aren't supported, if isEnabledForExtensions() == false"));
			}
		} else if (pObject instanceof DTO) {
			if (pConfig.isEnabledForExtensions()) {
				return new XmlRpcDTOSerializer();
			} else {
				throw new SAXException(new XmlRpcExtensionException("DTO objects aren't supported, if isEnabledForExtensions() == false"));
			}
		} else if (pObject instanceof Version) {
			if (pConfig.isEnabledForExtensions()) {
				return new XmlRpcVersionSerializer();
			} else {
				throw new SAXException(new XmlRpcExtensionException("Version objects aren't supported, if isEnabledForExtensions() == false"));
			}			
		}
		throw new SAXException(new XmlRpcExtensionException("Objects of class="+pObject.getClass().getName()+" are not serializable with XmlRpcTypeFactory"));
	}
	
	public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
		if (XmlRpcWriter.EXTENSIONS_URI.equals(pURI)) {
			if (!pConfig.isEnabledForExtensions()) {
				return null;
			}
			if (NullSerializer.NIL_TAG.equals(pLocalName)) {
				return new NullParser();
			} else if (I1Serializer.I1_TAG.equals(pLocalName)) {
				return new I1Parser();
			} else if (I2Serializer.I2_TAG.equals(pLocalName)) {
				return new I2Parser();
			} else if (I8Serializer.I8_TAG.equals(pLocalName)) {
				return new I8Parser();
			} else if (FloatSerializer.FLOAT_TAG.equals(pLocalName)) {
				return new FloatParser();
            } else if (NodeSerializer.DOM_TAG.equals(pLocalName)) {
                return new NodeParser();
            } else if (BigDecimalSerializer.BIGDECIMAL_TAG.equals(pLocalName)) {
                return new BigDecimalParser();
            } else if (BigIntegerSerializer.BIGINTEGER_TAG.equals(pLocalName)) {
                return new BigIntegerParser();
			} else if (SerializableSerializer.SERIALIZABLE_TAG.equals(pLocalName)) {
				return new XmlRpcSerializableParser();
			} else if ("osgidto".equals(pLocalName)) {
				return new XmlRpcSerializableParser();
			} else if ("osgiversion".equals(pLocalName)) {
				return new XmlRpcSerializableParser();
			} else if (CalendarSerializer.CALENDAR_TAG.equals(pLocalName)) {
			    return new CalendarParser();
            }
		} else if ("".equals(pURI)) {
			if (I4Serializer.INT_TAG.equals(pLocalName)  ||  I4Serializer.I4_TAG.equals(pLocalName)) {
				return new I4Parser();
			} else if (BooleanSerializer.BOOLEAN_TAG.equals(pLocalName)) {
				return new BooleanParser();
			} else if (DoubleSerializer.DOUBLE_TAG.equals(pLocalName)) {
				return new DoubleParser();
			} else if (DateSerializer.DATE_TAG.equals(pLocalName)) {
				return new DateParser(new XmlRpcDateTimeDateFormat(){
                    private static final long serialVersionUID = 7585237706442299067L;
                    protected TimeZone getTimeZone() {
                        return getController().getConfig().getTimeZone();
                    }
                });
			} else if (ObjectArraySerializer.ARRAY_TAG.equals(pLocalName)) {
				return new ObjectArrayParser(pConfig, pContext, this);
			} else if (MapSerializer.STRUCT_TAG.equals(pLocalName)) {
				return new MapParser(pConfig, pContext, this);
			} else if (ByteArraySerializer.BASE_64_TAG.equals(pLocalName)) {
				return new ByteArrayParser();
			} else if (StringSerializer.STRING_TAG.equals(pLocalName)) {
				return new StringParser();
			}
		}
		return null;
	}
}
