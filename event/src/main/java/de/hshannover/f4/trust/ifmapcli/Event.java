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
 * This file is part of ifmapcli (event), version 0.0.6, implemented by the Trust@HsH
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

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.net.ssl.TrustManager;

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
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.IfmapErrorResult;
import de.hshannover.f4.trust.ifmapj.exception.IfmapException;
import de.hshannover.f4.trust.ifmapj.exception.InitializationException;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.PublishNotify;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.EventType;
import de.hshannover.f4.trust.ifmapj.metadata.Significance;
import de.hshannover.f4.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

/**
 * A simple tool that can publish event metadata.
 *
 * Command line arguments specify the parameters.
 *
 * Environment variables define the connection details of the MAPS.
 *
 * @author ib
 *
 */
public class Event {
	final static String CMD = "event";

	private StandardIfmapMetadataFactory mMetaFac = IfmapJ.createStandardMetadataFactory();
	private SSRC mSsrc;
	private Document mEvent;
	PublishNotify mPubNotify;

	// CLI options parser stuff ( not the actual input params )
	Options mOptions;
	Option mIdentifier;
	Option mValue;
	Option mName;
	Option mHelp;

	// parsed command line options
	CommandLine mCmdLine;

	// configuration
	Config mConfig;

	/**
	 *
	 * @param args
	 * @throws FileNotFoundException
	 * @throws InitializationException
	 */
	public Event(String[] args) throws FileNotFoundException,
			InitializationException {
		mConfig = Common.loadEnvParams();
		parseCommandLine(args);
		preparePublishRequest();
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
		mSsrc.publish(Requests.createPublishReq(mPubNotify));
		mSsrc.endSession();
	}

	/**
	 * create {@link PublishRequest} object
	 */
	private void preparePublishRequest() {
		// create identifier depending on arguments
		Identifier identifier = null;
		IdentifierEnum type = IdentifierEnum.valueOf(mCmdLine
				.getOptionValue(mIdentifier.getOpt()));
		String value = mCmdLine.getOptionValue(mValue.getOpt());
		identifier = type.getIdentifier(value);

		// create event
		String eventName = mCmdLine.getOptionValue(mName.getOpt());
		mEvent  = mMetaFac.createEvent(eventName, "2011-08-19T09:09:21Z", "discId", new Integer(59), new Integer(50), Significance.important, EventType.policyViolation, null, "info", "http://www.example.org");

		mPubNotify = Requests.createPublishNotify(identifier, mEvent);

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
		mValue = OptionBuilder.create("v");
		mOptions.addOption(mValue);

		// name
		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("name");
		OptionBuilder.withDescription("name of event");
		mName = OptionBuilder.create("n");
		mOptions.addOption(mName);

		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			mCmdLine = parser.parse(mOptions, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp(
					Event.CMD + " -i <type> -v <value> [OPTION]...", mOptions);
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
			Event e = new Event(args);
			e.start();
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
