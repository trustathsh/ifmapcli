package de.fhhannover.inform.trust.ifmapcli;

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
 * This file is part of Ifmapcli, version 0.0.3, implemented by the Trust@FHH 
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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;

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

import de.fhhannover.inform.trust.ifmapcli.common.Common;
import de.fhhannover.inform.trust.ifmapcli.common.Config;
import de.fhhannover.inform.trust.ifmapcli.common.IdentifierEnum;
import de.fhhannover.inform.trust.ifmapj.IfmapJ;
import de.fhhannover.inform.trust.ifmapj.IfmapJHelper;
import de.fhhannover.inform.trust.ifmapj.binding.IfmapStrings;
import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.exception.InitializationException;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ifmapj.messages.ResultItem;
import de.fhhannover.inform.trust.ifmapj.messages.SearchRequest;
import de.fhhannover.inform.trust.ifmapj.messages.SearchResult;

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

	// ifmapj stuff
	SearchRequest mSearchRequest;
	SearchResult mSearchResult;
	Identifier startIdentifier;

	/**
	 * 
	 * @param args
	 * @throws FileNotFoundException
	 * @throws InitializationException
	 */
	public Search(String[] args) throws FileNotFoundException,
			InitializationException {
		mConfig = Common.loadEnvParams();
		parseCommandLine(args);
		prepareSearchRequest();
		initSsrc();
	}

	/**
	 * Create session, start search, parse results, end session
	 * 
	 * @throws IfmapErrorResult
	 * @throws IfmapException
	 */
	public void start() throws IfmapErrorResult, IfmapException {
		mSsrc.newSession();
		mSearchResult = mSsrc.search(mSearchRequest);
		parseSearchResult();
		mSsrc.endSession();
	}

	/**
	 * Fetch {@link ResultItem} objects and print them to console
	 */
	private void parseSearchResult() {

		Collection<ResultItem> resultItems = mSearchResult.getResultItems();
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

	/**
	 * create {@link SearchRequest} object
	 */
	private void prepareSearchRequest() {
		mSearchRequest = Requests.createSearchReq();

		// set start identifier
		Identifier startIdentifier = null;
		IdentifierEnum type = IdentifierEnum.valueOf(mCmdLine
				.getOptionValue(mIdentifier.getOpt()));
		String value = mCmdLine.getOptionValue(mValue.getOpt());
		startIdentifier = type.getIdentifier(value);
		mSearchRequest.setStartIdentifier(startIdentifier);

		// set match-links if necessary
		if (mCmdLine.hasOption(mMatchLinks.getOpt())) {
			mSearchRequest.setMatchLinksFilter(mCmdLine
					.getOptionValue(mMatchLinks.getOpt()));
		}

		// set max-depth if necessary
		if (mCmdLine.hasOption(mMaxDepth.getOpt())) {
			mSearchRequest.setMaxDepth(Integer.valueOf(mCmdLine
					.getOptionValue(mMaxDepth.getOpt())));
		}

		// set max-size if necessary
		if (mCmdLine.hasOption(mMaxSize.getOpt())) {
			mSearchRequest.setMaxSize(Integer.valueOf(mCmdLine
					.getOptionValue(mMaxSize.getOpt())));
		}

		// set result-filter if necessary
		if (mCmdLine.hasOption(mResultFilter.getOpt())) {
			mSearchRequest.setResultFilter(mCmdLine
					.getOptionValue(mResultFilter.getOpt()));
		}

		// set terminal-identifier-type if necessary
		if (mCmdLine.hasOption(mTerminalIdentifierTypes.getOpt())) {
			mSearchRequest.setTerminalIdentifierTypes(mCmdLine
					.getOptionValue(mTerminalIdentifierTypes.getOpt()));
		}

		// add default namespaces
		mSearchRequest.addNamespaceDeclaration(IfmapStrings.BASE_PREFIX,
				IfmapStrings.BASE_NS_URI);
		mSearchRequest.addNamespaceDeclaration(
				IfmapStrings.STD_METADATA_PREFIX,
				IfmapStrings.STD_METADATA_NS_URI);
		
		// add custom namespaces
		if (mCmdLine.hasOption(mNameSpacePrefix.getOpt()) && mCmdLine.hasOption(mNameSpaceURI.getOpt())) {
			mSearchRequest.addNamespaceDeclaration(
					mCmdLine.getOptionValue(mNameSpacePrefix.getOpt()),
					mCmdLine.getOptionValue(mNameSpaceURI.getOpt()));
		}

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
					Search.CMD + " -i <type> -v <value> [OPTION]...", mOptions);
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
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Search s = new Search(args);
			s.start();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (InitializationException e) {
			e.printStackTrace();
		} catch (IfmapErrorResult e) {
			e.printStackTrace();
		} catch (IfmapException e) {
			e.printStackTrace();
		}
	}

}
