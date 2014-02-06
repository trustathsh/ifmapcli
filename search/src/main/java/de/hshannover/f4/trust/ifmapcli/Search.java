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

import net.sourceforge.argparse4j.inf.ArgumentParser;
import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
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
public class Search extends AbstractClient {

	public static void main(String[] args) {
		command = "search";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addIdentifierType(parser, IdType.ipv4, IdType.ipv6, IdType.mac, IdType.dev, IdType.ar, IdType.id);
		ParserUtil.addIdentifier(parser);
		ParserUtil.addMatchLinks(parser);
		ParserUtil.addMaxDepth(parser);
		ParserUtil.addMaxSize(parser);
		ParserUtil.addResultFilter(parser);
		ParserUtil.addTerminalIdentifierType(parser);
		ParserUtil.addNamespacePrefix(parser);
		ParserUtil.addNamespaceUri(parser);

		parseParameters(parser, args);
		
		printParameters(new String[] {KEY_IDENTIFIER_TYPE, KEY_IDENTIFIER, KEY_MATCH_LINKS, KEY_MAX_DEPTH, KEY_MAX_SIZE, KEY_RESULT_FILTER, KEY_TERMINAL_IDENTIFIER_TYPE, KEY_NAMESPACE_PREFIX, KEY_NAMESPACE_URI});
		
		SearchRequest searchRequest = Requests.createSearchReq();

		// set start identifier
		IdType identifierType = resource.get(KEY_IDENTIFIER_TYPE);
		String identifierName = resource.getString(KEY_IDENTIFIER);
		Identifier startIdentifier = getIdentifier(identifierType, identifierName);
		searchRequest.setStartIdentifier(startIdentifier);
		
		String matchLinks = resource.getString(KEY_MATCH_LINKS);
		Integer maxDepth = resource.getInt(KEY_MAX_DEPTH); 
		Integer maxSize = resource.getInt(KEY_MAX_SIZE);
		String resultFilter = resource.getString(KEY_RESULT_FILTER);
		String terminalIdentifier = resource.getString(KEY_TERMINAL_IDENTIFIER_TYPE);
		String namespacePrefix = resource.getString(KEY_NAMESPACE_PREFIX);
		String namespaceUri = resource.getString(KEY_NAMESPACE_URI);

		// set match-links if necessary
		if (matchLinks != null) {
			searchRequest.setMatchLinksFilter(matchLinks);
		}

		// set max-depth if necessary
		if (maxDepth != null) {
			searchRequest.setMaxDepth(maxDepth);
		}

		// set max-size if necessary
		if (maxSize != null) {
			searchRequest.setMaxSize(maxSize);
		}

		// set result-filter if necessary
		if (resultFilter != null) {
			searchRequest.setResultFilter(resultFilter);
		}

		// set terminal-identifier-type if necessary
		if (terminalIdentifier != null) {
			searchRequest.setTerminalIdentifierTypes(terminalIdentifier);
		}

		// add default namespaces
		searchRequest.addNamespaceDeclaration(IfmapStrings.BASE_PREFIX,
			IfmapStrings.BASE_NS_URI);
		searchRequest.addNamespaceDeclaration(
			IfmapStrings.STD_METADATA_PREFIX,
			IfmapStrings.STD_METADATA_NS_URI);

		// add custom namespaces
		if ((namespacePrefix != null) && (namespaceUri != null)) {
			searchRequest.addNamespaceDeclaration(namespacePrefix, namespaceUri);
		}
		
		// search
		try {
			SSRC ssrc = createSSRC();
			ssrc.newSession();
			SearchResult searchResult = ssrc.search(searchRequest);
			parseSearchResult(searchResult);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
