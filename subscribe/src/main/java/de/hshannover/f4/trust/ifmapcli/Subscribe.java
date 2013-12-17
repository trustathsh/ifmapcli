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
 * This file is part of Ifmapcli, version 0.0.6, implemented by the Trust@HsH
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Vector;

import javax.net.ssl.TrustManager;
import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.Config;
import de.hshannover.f4.trust.ifmapcli.common.IdentifierEnum;
import de.hshannover.f4.trust.ifmapj.IfmapJ;
import de.hshannover.f4.trust.ifmapj.IfmapJHelper;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.ARC;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.EndSessionException;
import de.hshannover.f4.trust.ifmapj.exception.IfmapErrorResult;
import de.hshannover.f4.trust.ifmapj.exception.IfmapException;
import de.hshannover.f4.trust.ifmapj.exception.InitializationException;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.PollResult;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.messages.ResultItem;
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
public class Subscribe {
	final static String CMD = "subscribe";
	private static int counter = 0;

	// CLI options parser stuff ( not the actual input params )
	Options mOptions;
	Option mIdentifier;
	Option mValue;
	Option mMatchLinks;
	Option mMaxDepth;
	Option mMaxSize;
	Option mResultFilter;
	Option mTerminalIdentifierTypes;
	Option mHelp;
	Option mNameSpacePrefix;
	Option mNameSpaceURI;

	// parsed command line options
	CommandLine mCmdLine;

	// configuration
	Config mConfig;

	// SSRC
	SSRC mSsrc;

	// ARC
	ARC mArc;

	// ifmapj stuff
	SubscribeRequest mSubscribeRequest;
	PollResult mPollResult;
	Identifier startIdentifier;

	/**
	 *
	 * @param args
	 * @throws FileNotFoundException
	 * @throws InitializationException
	 */
	public Subscribe(String[] args) throws FileNotFoundException,
			InitializationException {
		mConfig = Common.loadEnvParams();
		parseCommandLine(args);
		prepareSubscribeRequest();
		initSsrc();
		initArc();
	}

	/**
	 * Create session
	 * Issue Subscription
	 * Poll for results and print them
	 *
	 * @throws IfmapErrorResult
	 * @throws IfmapException
	 * @throws EndSessionException
	 */
	public void start() throws IfmapErrorResult, IfmapException, EndSessionException {
		mSsrc.newSession();
		mSsrc.subscribe(mSubscribeRequest);
		while(true){
			System.out.println("Hit enter to proceed ...");
			try {
				new BufferedReader(new InputStreamReader(System.in)).readLine();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("Polling #" + Subscribe.counter++ + " ...");
			mPollResult = mArc.poll();
			parsePollResult();
	    	try {
	    		Thread.sleep(1000);
	    	} catch (InterruptedException e) {
	    		// ignore
	    	}
		}
	}

	/**
	 * Fetch poll results and print them to console
	 */
	private void parsePollResult() {

		// error, search, update, delete, notify
		Collection<IfmapErrorResult> errorRes = mPollResult.getErrorResults();
		Collection<SearchResult> allRes    = mPollResult.getResults();
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

	/**
	 * Parse {@link SearchResult} object and print it to console
	 */
	private void parseSearchResult(SearchResult sr) {
		for (ResultItem resultItem : sr.getResultItems()) {
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

	/**
	 * create {@link SubscribeRequest} object
	 */
	private void prepareSubscribeRequest() {
		mSubscribeRequest = Requests.createSubscribeReq();
		SubscribeUpdate su = Requests.createSubscribeUpdate();

		// set name
		su.setName("example-subscription");

		// set start identifier
		Identifier startIdentifier = null;
		IdentifierEnum type = IdentifierEnum.valueOf(mCmdLine
				.getOptionValue(mIdentifier.getOpt()));
		String value = mCmdLine.getOptionValue(mValue.getOpt());
		startIdentifier = type.getIdentifier(value);
		su.setStartIdentifier(startIdentifier);

		// set match-links if necessary
		if (mCmdLine.hasOption(mMatchLinks.getOpt())) {
			su.setMatchLinksFilter(mCmdLine
					.getOptionValue(mMatchLinks.getOpt()));
		}

		// set max-depth if necessary
		if (mCmdLine.hasOption(mMaxDepth.getOpt())) {
			su.setMaxDepth(Integer.valueOf(mCmdLine
					.getOptionValue(mMaxDepth.getOpt())));
		}

		// set max-size if necessary
		if (mCmdLine.hasOption(mMaxSize.getOpt())) {
			su.setMaxSize(Integer.valueOf(mCmdLine
					.getOptionValue(mMaxSize.getOpt())));
		}

		// set result-filter if necessary
		if (mCmdLine.hasOption(mResultFilter.getOpt())) {
			su.setResultFilter(mCmdLine
					.getOptionValue(mResultFilter.getOpt()));
		}

		// set terminal-identifier-type if necessary
		if (mCmdLine.hasOption(mTerminalIdentifierTypes.getOpt())) {
			su.setTerminalIdentifierTypes(mCmdLine
					.getOptionValue(mTerminalIdentifierTypes.getOpt()));
		}

		// add default namespaces
		su.addNamespaceDeclaration(IfmapStrings.BASE_PREFIX,
				IfmapStrings.BASE_NS_URI);
		su.addNamespaceDeclaration(
				IfmapStrings.STD_METADATA_PREFIX,
				IfmapStrings.STD_METADATA_NS_URI);

		// add custom namespaces
		if (mCmdLine.hasOption(mNameSpacePrefix.getOpt()) && mCmdLine.hasOption(mNameSpaceURI.getOpt())) {
			su.addNamespaceDeclaration(
					mCmdLine.getOptionValue(mNameSpacePrefix.getOpt()),
					mCmdLine.getOptionValue(mNameSpaceURI.getOpt()));
		}

		mSubscribeRequest.addSubscribeElement(su);
	}

	/**
	 * parse the command line by using Apache commons-cli
	 *
	 * @param args
	 */
	private void parseCommandLine(String[] args) {
		mOptions = new Options();
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(100);

		// boolean options
		mHelp = new Option("h", "help", false, "print this message");
		mOptions.addOption(mHelp);

		// argument options
		// identifier
		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("type");
		OptionBuilder.withDescription("abbreviated type of start identifier, "
				+ IdentifierEnum.ip + " | " + IdentifierEnum.mac + " | "
				+ IdentifierEnum.ar + " | " + IdentifierEnum.id + " | "
				+ IdentifierEnum.dev);
//		OptionBuilder.withLongOpt("identifier");
		OptionBuilder.withType(IdentifierEnum.class);
		mIdentifier = OptionBuilder.create("i");
		mOptions.addOption(mIdentifier);

		// value
		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("value");
		OptionBuilder.withDescription("value of identifier");
//		OptionBuilder.withLongOpt("value");
		mValue = OptionBuilder.create("v");
		mOptions.addOption(mValue);

		// match-links
		OptionBuilder.hasArg();
		OptionBuilder.withArgName("filter");
		OptionBuilder
				.withDescription("filter for match-links. default is match-all. example: meta:ip-mac");
//		OptionBuilder.withLongOpt("match-links");
		mMatchLinks = OptionBuilder.create("ml");
		mOptions.addOption(mMatchLinks);

		// max-depth
		OptionBuilder.hasArg();
		OptionBuilder.withArgName("depth");
		OptionBuilder
				.withDescription("maximum depth for search. default is 0");
//		OptionBuilder.withLongOpt("max-depth");
		mMaxDepth = OptionBuilder.create("md");
		mOptions.addOption(mMaxDepth);

		// max-size
		OptionBuilder.hasArg();
		OptionBuilder.withArgName("size");
		OptionBuilder
				.withDescription("maximum size for search results. default=based on MAPS");
//		OptionBuilder.withLongOpt("max-size");
		mMaxSize = OptionBuilder.create("ms");
		mOptions.addOption(mMaxSize);

		// result-filter
		OptionBuilder.hasArg();
		OptionBuilder.withArgName("filter");
		OptionBuilder
				.withDescription("result-filter for search results. default=match-all. example: meta:ip-mac");
//		OptionBuilder.withLongOpt("result-filter");
		mResultFilter = OptionBuilder.create("rf");
		mOptions.addOption(mResultFilter);

		// terminal-identifier-type
		OptionBuilder.hasArg();
		OptionBuilder.withArgName("t1,t2,...");
		OptionBuilder.withDescription("comma separated list of identifier types: "
				+ "ip-address,mac-address,device,access-request,identity");
//		OptionBuilder.withLongOpt("terminal-identifier-types");
		mTerminalIdentifierTypes = OptionBuilder.create("tit");
		mOptions.addOption(mTerminalIdentifierTypes);

		// namespace declarations: prefix
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("custom namespace prefix. example: foo");
//		OptionBuilder.withLongOpt("namespace-prefix");
		mNameSpacePrefix = OptionBuilder.create("nP");
		mOptions.addOption(mNameSpacePrefix);

		// namespace declarations: URI
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("custom namespace URI. example: http://www.foo.bar/2012/ifmap-metadata/1");
//		OptionBuilder.withLongOpt("namespace-uri");
		mNameSpaceURI = OptionBuilder.create("nU");
		mOptions.addOption(mNameSpaceURI);

		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			mCmdLine = parser.parse(mOptions, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp(
					Subscribe.CMD + " -i <type> -v <value> [OPTION]...", mOptions);
			System.out.println(Common.USAGE);
			System.exit(1);
		}
	}

	/**
	 * Load {@link TrustManager} instances and create {@link SSRC}.
	 *
	 * @throws FileNotFoundException
	 * @throws InitializationException
	 */
	private void initSsrc() throws FileNotFoundException,
			InitializationException {
		InputStream is = Common
				.prepareTruststoreIs(mConfig.getTruststorePath());
		TrustManager[] tms = IfmapJHelper.getTrustManagers(is,
				mConfig.getTruststorePass());
		mSsrc = IfmapJ.createSSRC(mConfig.getUrl(), mConfig.getUser(),
				mConfig.getPass(), tms);
	}

	/**
	 * @throws InitializationException
	 */
	private void initArc() throws InitializationException {
		mArc = mSsrc.getArc();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Subscribe s = new Subscribe(args);
			s.start();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (InitializationException e) {
			e.printStackTrace();
		} catch (IfmapErrorResult e) {
			e.printStackTrace();
		} catch (IfmapException e) {
			e.printStackTrace();
		} catch (EndSessionException e) {
			e.printStackTrace();
		}
	}

}
