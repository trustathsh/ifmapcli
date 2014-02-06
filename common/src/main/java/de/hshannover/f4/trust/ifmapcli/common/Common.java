/*
 * #%L
 * =====================================================
 *   _____                _     ____  _   _       _   _
 *  |_   _|_ __ _   _ ___| |_  / __ \| | | | ___ | | | |
 *    | | | '__| | | / __| __|/ / _` | |_| |/ __|| |_| |
 *    | | | |  | |_| \__ \ |_| | (_| |  _  |\__ \|  _  |
 *    |_| |_|   \__,_|___/\__|\ \__,_|_| |_||___/|_| |_|
 *                             \____/
 * 
 * =====================================================
 * 
 * Hochschule Hannover
 * (University of Applied Sciences and Arts, Hannover)
 * Faculty IV, Dept. of Computer Science
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 * 
 * Email: trust@f4-i.fh-hannover.de
 * Website: http://trust.f4.hs-hannover.de
 * 
 * This file is part of ifmapcli (common), version 0.0.6, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2013 Trust@HsH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.hshannover.f4.trust.ifmapcli.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * Helper class.
 *
 * @author ib
 *
 */
public class Common {

	/**
	 * Prepare access to truststore by creating an InputStream. This supports<br/>
	 * both truststores that reside within the packaged jar as well as those<br/>
	 * that reside on the local filesystem separately from the jar.<br/>
	 *
	 * @param path - path to the keystore
	 * @return
	 * @throws FileNotFoundException
	 */
	public static InputStream prepareTruststoreIs(String path) throws FileNotFoundException{
		InputStream is;
		// try jar
		is = Common.class.getResourceAsStream(path);
		if(is == null){
			System.out.print("Truststore " + path + " not " +
					"found in jar. Will try filesystem now... ");
			// try filesystem
			is = new FileInputStream(new File(path));
			System.out.println("success!");
		}

		return is;
	}

	/**
	 * Format the Date as xsd:DateTime.
	 *
	 * @param d - the date the is to be formatted
	 * @return the xsd:DateTime String, e.g. 2003-05-31T13:20:05-05:00
	 */
	public static String getTimeAsXsdDateTime(Date d){
		// this uses ugly hacks since we do not want to rely on JAXB
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		SimpleDateFormat sdf2 = new SimpleDateFormat("Z");
		String one = sdf1.format(d.getTime());
		String offset = sdf2.format(d.getTime());
		String offsetA = offset.substring(0, 3);
		String offsetB = offset.substring(3);
		return one + offsetA + ":" + offsetB;
	}

	/**
	 * 'Easy' Java way to transform a {@link Document} to a {@link String}
	 *
	 * @param doc
	 * @return
	 * @throws TransformerException
	 */
	public static String documentToString(Document doc) throws TransformerException{
			// oh well ...
			StreamResult result = new StreamResult(new StringWriter());
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(new DOMSource(doc), result);
			return result.getWriter().toString();
	}

}
