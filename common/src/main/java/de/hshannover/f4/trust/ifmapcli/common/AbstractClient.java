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
 * This file is part of ifmapcli (role), version 0.0.6, implemented by the Trust@HsH
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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;

import javax.net.ssl.TrustManager;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import de.hshannover.f4.trust.ifmapj.IfmapJ;
import de.hshannover.f4.trust.ifmapj.IfmapJHelper;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.InitializationException;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.identifier.IdentityType;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.ResultItem;
import de.hshannover.f4.trust.ifmapj.messages.SearchResult;
import de.hshannover.f4.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

public abstract class AbstractClient {

	protected enum IdType {
		ipv4, ipv6, mac, dev, ar, id
	}
	
	protected static String command;
	
	protected static Namespace resource;
	
	// in order to create the necessary objects, make use of the appropriate
	// factory classes
	protected static StandardIfmapMetadataFactory mf = IfmapJ
			.createStandardMetadataFactory();

	protected static ArgumentParser createDefaultParser() {
		ArgumentParser parser = ArgumentParsers.newArgumentParser(command);
		
		parser.defaultHelp(true);
		ParserUtil.addConnectionArgumentsTo(parser);
		ParserUtil.addCommonArgumentsTo(parser);
		
		return parser;
	}
	
	protected static void parseParameters(ArgumentParser parser, String[] arguments) {
		try {
			resource = parser.parseArgs(arguments);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}
	}
	
	protected static void printParameters(String operation, String[] keys) {
		if (resource.getBoolean(ParserUtil.VERBOSE)) {
			StringBuilder sb = new StringBuilder();
			
			sb.append(command).append(" ");
			if (operation != null) {				
				sb.append(resource.getString(operation)).append(" ");
			}
			if (keys != null) {				
				for (String key : keys) {
					appendIfNotNull(sb, resource, key);
				}
			}
			
			appendConnectionArguments(sb, resource);
			System.out.println(sb.toString());
		}
	}
	
	protected static void printParameters(String[] keys) {
		printParameters(null, keys);
	}
	
	protected static SSRC createSSRC() throws FileNotFoundException, InitializationException {
		InputStream is = Common.prepareTruststoreIs(resource.getString(ParserUtil.KEYSTORE_PATH));
		TrustManager[] tms = IfmapJHelper.getTrustManagers(is, resource.getString(ParserUtil.KEYSTORE_PASS));
		SSRC ssrc = IfmapJ.createSSRC(
			resource.getString(ParserUtil.URL),
			resource.getString(ParserUtil.USER),
			resource.getString(ParserUtil.PASS),
			tms);
		
		return ssrc;
	}
	
	protected static void publishIfmapData(PublishRequest request) {
		try {			
			SSRC ssrc = createSSRC();
			ssrc.newSession();
			ssrc.publish(request);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	protected static boolean isUpdate(String key) {
		return resource.getString(key).equals("update");
	}
	
	protected static boolean isNotify(String key) {
		return resource.getString(key).equals("notify");
	}
	
	protected static boolean isDelete(String key) {
		return resource.getString(key).equals("delete");
	}
	
	protected static Identifier getIdentifier(IdType type, String name) {
		switch (type) {
		case ipv4:
			return Identifiers.createIp4(name);
		case ipv6:
			return Identifiers.createIp6(name);
		case id:
			// TODO add optinal parameter for the identity identifier type
			return Identifiers.createIdentity(IdentityType.other, name);
		case mac:
			return Identifiers.createMac(name);
		case dev:
			return Identifiers.createDev(name);
		case ar:
			return Identifiers.createAr(name);
		default:
			throw new RuntimeException("unknown identifier type '" + type + "'");
		}
	}
	
	public static void appendConnectionArguments(StringBuilder sb, Namespace res) {
		appendIfNotNull(sb, res, ParserUtil.URL);
		appendIfNotNull(sb, res, ParserUtil.USER);
		appendIfNotNull(sb, res, ParserUtil.PASS);
		appendIfNotNull(sb, res, ParserUtil.KEYSTORE_PATH);
		appendIfNotNull(sb, res, ParserUtil.KEYSTORE_PASS);
	}
	
	public static void appendIfNotNull(StringBuilder sb, Namespace resource,
			String key) {
		Object property = resource.get(key);
		if (property != null) {			
			if (property instanceof String) {
				sb.append(key).append("=").append(resource.getString(key)).append(" ");
			} else if (property instanceof Integer) {
				sb.append(key).append("=").append(resource.getInt(key)).append(" ");
			} else {
				sb.append(key).append("=").append(property).append(" ");
			}
		}
	}
	
	/**
	 * Fetch {@link ResultItem} objects and print them to console
	 * @param searchResult 
	 */
	protected static void parseSearchResult(SearchResult searchResult) {

		Collection<ResultItem> resultItems = searchResult.getResultItems();
		for (ResultItem resultItem : resultItems) {
			System.out.println("****************************************************************************");
			System.out.println(resultItem);
			Collection<Document> meta = resultItem.getMetadata();
			for (Document document : meta) {
				try {
					System.out.println(Common.documentToString(document));
				} catch (TransformerException e) {
					e.printStackTrace();
				}
			}
			System.out.println("****************************************************************************");
		}
	}
}
