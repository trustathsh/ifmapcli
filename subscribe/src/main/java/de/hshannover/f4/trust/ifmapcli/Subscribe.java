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
 * This file is part of ifmapcli (subscribe), version 0.2.1, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2015 Trust@HsH
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Vector;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.ARC;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.IfmapErrorResult;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.PollResult;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.messages.SearchResult;
import de.hshannover.f4.trust.ifmapj.messages.SubscribeRequest;
import de.hshannover.f4.trust.ifmapj.messages.SubscribeUpdate;

/**
 * A simple tool that does an IF-MAP subscribe operation and polls for results.<br/>
 * The results are printed to the command line. Each time a pollResult is<br/>
 * received, a new poll is sent.
 *
 * Command line arguments specify the subscribe parameters.
 *
 * Environment variables define the connection details of the MAPS.
 *
 * @author ib
 *
 */
public class Subscribe extends AbstractClient {

	private static int counter = 0;

	/**
	 * Fetch poll results and print them to console
	 * @param pollResult 
	 */
	private static void parsePollResult(PollResult pollResult) {

		// error, search, update, delete, notify
		Collection<IfmapErrorResult> errorRes = pollResult.getErrorResults();
		Collection<SearchResult> allRes    = pollResult.getResults();
		Collection<SearchResult> searchRes = new Vector<SearchResult>();
		Collection<SearchResult> updateRes = new Vector<SearchResult>();
		Collection<SearchResult> deleteRes = new Vector<SearchResult>();
		Collection<SearchResult> notifyRes = new Vector<SearchResult>();

		for (SearchResult res : allRes) {
			switch (res.getType()) {
			case searchResult:
				searchRes.add(res);
				break;
			case updateResult:
				updateRes.add(res);
				break;
			case deleteResult:
				deleteRes.add(res);
				break;
			case notifyResult:
				notifyRes.add(res);
				break;
			default:
				break;
			}
		}

		if(errorRes.size() > 0){
			System.err.println("== ERROR RESULTS ==");
			for (IfmapErrorResult error : errorRes) {
				System.err.println(error);
			}
			System.exit(1);
		}

		if(searchRes.size() > 0){
			System.out.println("== SEARCH RESULTS ==");
			for (SearchResult searchResult : searchRes) {
				parseSearchResult(searchResult);
			}
		}

		if(updateRes.size() > 0){
			System.out.println("== UPDATE RESULTS ==");
			for (SearchResult searchResult : updateRes) {
				parseSearchResult(searchResult);
			}
		}

		if(deleteRes.size() > 0){
			System.out.println("== DELETE RESULTS ==");
			for (SearchResult searchResult : deleteRes) {
				parseSearchResult(searchResult);
			}
		}

		if(notifyRes.size() > 0){
			System.out.println("== NOTIFY RESULTS ==");
			for (SearchResult searchResult : notifyRes) {
				parseSearchResult(searchResult);
			}
		}
	}	

	public static void main(String[] args) {
		command = "subscribe";
		
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
		
		SubscribeRequest subscribeRequest = Requests.createSubscribeReq();
		SubscribeUpdate su = Requests.createSubscribeUpdate();

		// set name
		su.setName("example-subscription");

		// set start identifier
		IdType identifierType = resource.get(KEY_IDENTIFIER_TYPE);
		String identifierName = resource.getString(KEY_IDENTIFIER);
		Identifier startIdentifier = getIdentifier(identifierType, identifierName);
		su.setStartIdentifier(startIdentifier);
		
		String matchLinks = resource.getString(KEY_MATCH_LINKS);
		Integer maxDepth = resource.getInt(KEY_MAX_DEPTH); 
		Integer maxSize = resource.getInt(KEY_MAX_SIZE);
		String resultFilter = resource.getString(KEY_RESULT_FILTER);
		String terminalIdentifier = resource.getString(KEY_TERMINAL_IDENTIFIER_TYPE);
		String namespacePrefix = resource.getString(KEY_NAMESPACE_PREFIX);
		String namespaceUri = resource.getString(KEY_NAMESPACE_URI);

		// set match-links if necessary
		if (matchLinks != null) {
			su.setMatchLinksFilter(matchLinks);
		}

		// set max-depth if necessary
		if (maxDepth != null) {
			su.setMaxDepth(maxDepth);
		}

		// set max-size if necessary
		if (maxSize != null) {
			su.setMaxSize(maxSize);
		}

		// set result-filter if necessary
		if (resultFilter != null) {
			su.setResultFilter(resultFilter);
		}

		// set terminal-identifier-type if necessary
		if (terminalIdentifier != null) {
			su.setTerminalIdentifierTypes(terminalIdentifier);
		}

		// add default namespaces
		su.addNamespaceDeclaration(IfmapStrings.BASE_PREFIX,
			IfmapStrings.BASE_NS_URI);
		su.addNamespaceDeclaration(
			IfmapStrings.STD_METADATA_PREFIX,
			IfmapStrings.STD_METADATA_NS_URI);

		// add custom namespaces
		if ((namespacePrefix != null) && (namespaceUri != null)) {
			su.addNamespaceDeclaration(namespacePrefix, namespaceUri);
		}

		subscribeRequest.addSubscribeElement(su);
		
		try {
			SSRC ssrc = createSSRC();
			ssrc.newSession();
			ARC mArc = ssrc.getArc();
			
			ssrc.newSession();
			ssrc.subscribe(subscribeRequest);
			while (true) {
				System.out.println("Hit enter to proceed ...");
				try {
					new BufferedReader(new InputStreamReader(System.in)).readLine();
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(-1);
				}
				System.out.println("Polling #" + Subscribe.counter++ + " ...");
				PollResult pollResult = mArc.poll();
				parsePollResult(pollResult);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}	
}
