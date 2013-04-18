package de.fhhannover.inform.trust.ifmapcli.common;

/*
 * #%L
 * ====================================================
 *   _____                _     ____  _____ _   _ _   _
 *  |_   _|_ __ _   _ ___| |_  / __ \|  ___| | | | | | |
 *    | | | '__| | | / __| __|/ / _` | |_  | |_| | |_| |
 *    | | | |  | |_| \__ \ |_| | (_| |  _| |  _  |  _  |
 *    |_| |_|   \__,_|___/\__|\ \__,_|_|   |_| |_|_| |_|
 *                             \____/
 * 
 * =====================================================
 * 
 * Fachhochschule Hannover 
 * (University of Applied Sciences and Arts, Hannover)
 * Faculty IV, Dept. of Computer Science
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 * 
 * Email: trust@f4-i.fh-hannover.de
 * Website: http://trust.inform.fh-hannover.de/
 * 
 * This file is part of Ifmapcli, version 0.0.2, implemented by the Trust@FHH 
 * research group at the Fachhochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2013 Trust@FHH
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
	
	public static final String USAGE = 
			"\nYou may also set the enviroment variables\n" +
			"IFMAP_URL, IFMAP_USER, IFMAP_PASS, IFMAP_TRUSTSTORE_PATH and IFMAP_TRUSTSTORE_PASS\n" +
			"\nIf you don't specify them, the following default values will be used:\n" +
			"\tIFMAP_URL=" + Config.DEFAULT_URL + "\n" +
			"\tIFMAP_USER=" + Config.DEFAULT_USER + "\n" +
			"\tIFMAP_PASS=" + Config.DEFAULT_PASS + "\n" +
			"\tIFMAP_TRUSTSTORE_PATH=" + Config.DEFAULT_TRUSTSTORE_PATH + "\n" +
			"\tIFMAP_TRUSTSTORE_PASS=" + Config.DEFAULT_TRUSTSTORE_PASS;

	/**
	 * Check if the given op String equals 'update'.
	 * 
	 * @param op
	 * @return
	 */
	public static boolean isUpdate(String op){
		return ("update".equals(op));
	}
	
	/**
	 * Check if the given op String equals 'delete'.
	 * 
	 * @param op
	 * @return
	 */
	public static boolean isDelete(String op){
		return ("delete".equals(op));
	}
	
	/**
	 * Check if the given op String equals 'update' or 'delete'.
	 * 
	 * @param op
	 * @return
	 */
	public static boolean isUpdateorDelete(String op){
		return Common.isUpdate(op) || Common.isDelete(op);
	}
	
	/**
	 * Checks for the necessary parameters that were provided by args or as<br/>
	 * environment variables.
	 * 
	 * @param args - the command line arguments
	 * @param expectedArgc - expected number of command line arguments (if there
	 * 						are any)
	 * @return the loaded configuration
	 */
	public static Config checkAndLoadParams(String[] args, int expectedArgc) {
		Config cfg;
		
		if (args.length == expectedArgc){
			// load parameters from args
			cfg = loadCmdParams(args);
		} else {
			cfg = loadEnvParams();
		}
		
		return cfg;
	}
	
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
					"found in jar. Will try filesystem now...");
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
	 * Load parameters from command line and create {@link Config} object.<br/>
	 * This assumes that the last 5 parameters of any command that uses this<br/>
	 * method are: url, user, pass, truststorePath, truststorePass
	 * @param args
	 * @return
	 */
	private static Config loadCmdParams(String[] args) {
		String url = args[args.length-5];
		String user = args[args.length-4];
		String pass = args[args.length-3];
		String truststorePath = args[args.length-2];
		String truststorePass = args[args.length-1];
		return new Config(url, user, pass, truststorePath, truststorePass);
	}
	
	/**
	 * Load parameters from environment variables and create {@link Config} object.
	 * @param args
	 * @return
	 */
	public static Config loadEnvParams() {
		// create default configuration
		Config cfg = new Config();
		
		// get environment variables
		String url = System.getenv("IFMAP_URL");
		String user = System.getenv("IFMAP_USER");
		String pass = System.getenv("IFMAP_PASS");
		String truststorePath = System.getenv("IFMAP_TRUSTSTORE_PATH");
		String truststorePass = System.getenv("IFMAP_TRUSTSTORE_PASS");
		
		// set them if they were defined
		if (url != null) cfg.setUrl(url);
		if (user != null) cfg.setUser(user);
		if (pass != null) cfg.setPass(pass);
		if (truststorePath != null) cfg.setTruststorePath(truststorePath);
		if (truststorePass != null) cfg.setTruststorePass(truststorePass);

		return cfg;
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
