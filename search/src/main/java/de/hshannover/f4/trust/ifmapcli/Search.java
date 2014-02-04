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
 * This file is part of ifmapcli (search), version 0.0.6, implemented by the Trust@HsH
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
package de.hshannover.f4.trust.ifmapcli;

import java.io.InputStream;
import java.util.Collection;

import javax.net.ssl.TrustManager;
import javax.xml.transform.TransformerException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapj.IfmapJ;
import de.hshannover.f4.trust.ifmapj.IfmapJHelper;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.identifier.IdentityType;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.messages.ResultItem;
import de.hshannover.f4.trust.ifmapj.messages.SearchRequest;
import de.hshannover.f4.trust.ifmapj.messages.SearchResult;

/**
 * A simple tool that does an IF-MAP search. The result is printed to the
 * command line.
 *
 * Command line arguments specify the search parameters.
 *
 * Environment variables define the connection details of the MAPS.
 *
 * @author ib
 *
 */
public class Search {
	final static String CMD = "search";

	enum IdType {
		ipv4, ipv6, mac, dev, ar, id
	}
	
	/**
	 * Fetch {@link ResultItem} objects and print them to console
	 * @param searchResult 
	 */
	private static void parseSearchResult(SearchResult searchResult) {

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

	public static void main(String[] args) {
		final String KEY_IDENTIFIER = "identifier";
		final String KEY_IDENTIFIER_TYPE = "identifierType";
		final String KEY_MATCH_LINKS = "matchLinks";
		final String KEY_MAX_DEPTH = "maxDepth";
		final String KEY_MAX_SIZE = "maxSize";
		final String KEY_RESULT_FILTER = "resultFilter";
		final String KEY_TERMINAL_IDENTIFIER_TYPE = "terminal-identifier-type";
		final String KEY_NAMESPACE_PREFIX = "namespacePrefix";
		final String KEY_NAMESPACE_URI = "namespaceUri";

		ArgumentParser parser = ArgumentParsers.newArgumentParser(CMD);
		parser.addArgument("identifier-type")
		.type(IdType.class)
		.dest(KEY_IDENTIFIER_TYPE)
		.choices(
			IdType.ipv4,
			IdType.ipv6,
			IdType.mac,
			IdType.dev,
			IdType.ar,
			IdType.id)
			.help("the type of the identifier");
		parser.addArgument("identifier")
			.type(String.class)
			.dest(KEY_IDENTIFIER)
			.help("the identifier");
		parser.addArgument("--match-links", "-ml")
			.type(String.class)
			.dest(KEY_MATCH_LINKS)
			.help("filter for match-links. default is match-all (example: meta:ip-mac)");
		parser.addArgument("--max-depth", "-md")
			.type(Integer.class)
			.dest(KEY_MAX_DEPTH)
			.setDefault(0)
			.help("maximum depth for search. default is 0");
		parser.addArgument("--max-size", "-ms")
			.type(Integer.class)
			.dest(KEY_MAX_SIZE)
			.help("maximum size for search results. default=based on MAPS");
		parser.addArgument("--result-filter", "-rf")
			.type(String.class)
			.dest(KEY_RESULT_FILTER)
			.help("result-filter for search results. default=match-all. example: meta:ip-mac");
		parser.addArgument("--terminal-identifier-type", "-tt")
			.type(String.class)
			.dest(KEY_TERMINAL_IDENTIFIER_TYPE)
			.help("comma-separated type of the terminal identifier(s): ip-address,mac-address,device,access-request,identity");
		parser.addArgument("--namespace-prefix", "-nP")
			.type(String.class)
			.dest(KEY_NAMESPACE_PREFIX)
			.help("custom namespace prefix. example: foo");		
		parser.addArgument("--namespace-uri", "-nU")
			.type(String.class)
			.dest(KEY_NAMESPACE_URI)
			.help("custom namespace URI. example: http://www.foo.bar/2012/ifmap-metadata/1");
		ParserUtil.addConnectionArgumentsTo(parser);
		ParserUtil.addCommonArgumentsTo(parser);

		Namespace res = null;
		try {
			res = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		if (res.getBoolean(ParserUtil.VERBOSE)) {
			StringBuilder sb = new StringBuilder();
			
			sb.append(CMD).append(" ");
			sb.append(KEY_IDENTIFIER_TYPE).append("=").append(res.get(KEY_IDENTIFIER_TYPE)).append(" ");
			sb.append(KEY_IDENTIFIER).append("=").append(res.getString(KEY_IDENTIFIER)).append(" ");
			ParserUtil.appendStringIfNotNull(sb, res, KEY_MATCH_LINKS);
			ParserUtil.appendIntegerIfNotNull(sb, res, KEY_MAX_DEPTH);
			ParserUtil.appendIntegerIfNotNull(sb, res, KEY_MAX_SIZE);
			ParserUtil.appendStringIfNotNull(sb, res, KEY_RESULT_FILTER);
			ParserUtil.appendStringIfNotNull(sb, res, KEY_TERMINAL_IDENTIFIER_TYPE);
			ParserUtil.appendStringIfNotNull(sb, res, KEY_NAMESPACE_PREFIX);
			ParserUtil.appendStringIfNotNull(sb, res, KEY_NAMESPACE_URI);
			
			ParserUtil.printConnectionArguments(sb, res);
			System.out.println(sb.toString());
		}
		
		SearchRequest searchRequest = Requests.createSearchReq();

		// set start identifier
		IdType identifierType = res.get(KEY_IDENTIFIER_TYPE);
		String identifierName = res.getString(KEY_IDENTIFIER);
		Identifier startIdentifier = getIdentifier(identifierType, identifierName);
		searchRequest.setStartIdentifier(startIdentifier);

		// set match-links if necessary
		if (res.getString(KEY_MATCH_LINKS) != null) {
			searchRequest.setMatchLinksFilter(res.getString(KEY_MATCH_LINKS));
		}

		// set max-depth if necessary
		if (res.getInt(KEY_MAX_DEPTH) != null) {
			searchRequest.setMaxDepth(res.getInt(KEY_MAX_DEPTH));
		}

		// set max-size if necessary
		if (res.getInt(KEY_MAX_SIZE) != null) {
			searchRequest.setMaxSize(res.getInt(KEY_MAX_SIZE));
		}

		// set result-filter if necessary
		if (res.getString(KEY_RESULT_FILTER) != null) {
			searchRequest.setResultFilter(res.getString(KEY_RESULT_FILTER));
		}

		// set terminal-identifier-type if necessary
		if (res.getString(KEY_TERMINAL_IDENTIFIER_TYPE) != null) {
			searchRequest.setTerminalIdentifierTypes(res.getString(KEY_TERMINAL_IDENTIFIER_TYPE));
		}

		// add default namespaces
		searchRequest.addNamespaceDeclaration(IfmapStrings.BASE_PREFIX,
			IfmapStrings.BASE_NS_URI);
		searchRequest.addNamespaceDeclaration(
			IfmapStrings.STD_METADATA_PREFIX,
			IfmapStrings.STD_METADATA_NS_URI);

		// add custom namespaces
		if ((res.getString(KEY_NAMESPACE_PREFIX) != null) && (res.getString(KEY_NAMESPACE_URI) != null)) {
			searchRequest.addNamespaceDeclaration(
				res.getString(KEY_NAMESPACE_PREFIX),
				res.getString(KEY_NAMESPACE_URI));
		}
		
		// search
		try {
			InputStream is = Common.prepareTruststoreIs(res.getString(ParserUtil.KEYSTORE_PATH));
			TrustManager[] tms = IfmapJHelper.getTrustManagers(is, res.getString(ParserUtil.KEYSTORE_PASS));
			SSRC ssrc = IfmapJ.createSSRC(
				res.getString(ParserUtil.URL),
				res.getString(ParserUtil.USER),
				res.getString(ParserUtil.PASS),
				tms);
			ssrc.newSession();
			SearchResult searchResult = ssrc.search(searchRequest);
			parseSearchResult(searchResult);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static Identifier getIdentifier(IdType type, String name) {
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
}
