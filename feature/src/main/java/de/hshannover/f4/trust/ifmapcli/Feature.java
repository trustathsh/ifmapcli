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
 * This file is part of ifmapcli (feature), version 0.0.6, implemented by the Trust@HsH
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.net.ssl.TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.Config;
import de.hshannover.f4.trust.ifmapj.IfmapJ;
import de.hshannover.f4.trust.ifmapj.IfmapJHelper;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.EndSessionException;
import de.hshannover.f4.trust.ifmapj.exception.IfmapErrorResult;
import de.hshannover.f4.trust.ifmapj.exception.IfmapException;
import de.hshannover.f4.trust.ifmapj.exception.InitializationException;
import de.hshannover.f4.trust.ifmapj.identifier.Device;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.identifier.Identity;
import de.hshannover.f4.trust.ifmapj.identifier.IdentityType;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple publisher implementation that publishes random feature metadata with
 * lifetime 'forever'.
 * To delete the metadata use the ifmapcli purge tool.
 *
 * @author Ralf Steuerwald
 *
 */
public class Feature {

	final static String CMD = "feature";

	final static String OTHER_TYPE_DEFINITION = "32939:category";
	final static String NAMESPACE = "http://www.esukom.de/2012/ifmap-metadata/1";
	final static String NAMESPACE_PREFIX = "esukom";

	private SSRC mSsrc;

	private String mDeviceIdentifier;
	private int mTreeDepth;
	private int mMaxChildsPerCategory;
	private int mMaxFeaturePerCategory;

	private List<PublishUpdate> mPublishUpdates = new ArrayList<PublishUpdate>();

	private DocumentBuilderFactory mDocumentBuilderFactory;
	private DocumentBuilder mDocumentBuilder;

	private List<Double> mFeatureValues = new ArrayList<Double>();
	private Random mFeatureRandom = new Random(42L);

	// CLI options parser stuff ( not the actual input params )
	Options mOptions;

	Option mDeviceIdentifierOption;
	Option mTreeDepthOption;
	Option mMaxChildsPerCategoryOption;
	Option mMaxFeaturePerCategoryOption;


	Option mNameOp;
	Option mHelpOp;

	// parsed command line options
	CommandLine mCmdLine;

	// configuration
	Config mConfig;

	public Feature(String[] args) throws FileNotFoundException,
			InitializationException {
		mConfig = Common.loadEnvParams();
		System.out.println(CMD + " uses config " + mConfig);
		parseCommandLine(args);
		initSsrc();

		mDocumentBuilderFactory = DocumentBuilderFactory.newInstance();

		try {
			mDocumentBuilder = mDocumentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		preparePublishUpdates();
	}

	/**
	 * Create session, start publishing, end session, print stats
	 *
	 * @throws IfmapErrorResult
	 * @throws IfmapException
	 * @throws EndSessionException
	 */
	public void start() throws IfmapErrorResult, IfmapException, EndSessionException {
		mSsrc.newSession();

		PublishRequest req = Requests.createPublishReq();

		for (PublishUpdate u : mPublishUpdates) {
			req.addPublishElement(u);
		}

		mSsrc.publish(req);
		mSsrc.endSession();

		printStats();
	}

	private void preparePublishUpdates() {
		PublishUpdate update = Requests.createPublishUpdate();

		String nodeName = "rootCategory";
		Device dev = Identifiers.createDev(mDeviceIdentifier);
		Identity root = createCategory(nodeName, mDeviceIdentifier);

		Document deviceCategory = createCategoryLink("device-category");

		update.setIdentifier1(dev);
		update.setIdentifier2(root);
		update.addMetadata(deviceCategory);
		update.setLifeTime(MetadataLifetime.forever);

		mPublishUpdates.add(update);

		fork(root, 0, new String[] {nodeName});
	}

	/**
	 * Creates the child nodes for the given parent {@link Identity}. The
	 * resulting {@link PublishUpdate}s for each child are added to
	 * <code>mPublishUpdates</code>.
	 *
	 * @param parent
	 * @param currentDepth
	 */
	private void fork(Identity parent, int currentDepth, String[] path) {
		if (currentDepth < mTreeDepth) {
			int childCount = new Random().nextInt(mMaxChildsPerCategory) + 1;

			for (int i = 0; i < childCount; i++) {
				PublishUpdate update = Requests.createPublishUpdate();

				String name = "category" + ((i % 4 == 0) ? ("X:" + (i / 4)) : i + "");
				String fullNodeName = joinStrings(path) + "." + name;
				Identity node = createCategory(fullNodeName, mDeviceIdentifier);

				Document subCategoryOf = createCategoryLink("subcategory-of");

				update.setIdentifier1(parent);
				update.setIdentifier2(node);
				update.addMetadata(subCategoryOf);
				update.setLifeTime(MetadataLifetime.forever);

				mPublishUpdates.add(update);
				appendFeatures(node);

				fork(node, currentDepth+1, append(name, path));
			}
		}
	}

	private String joinStrings(String[] parts) {
		StringBuffer s = new StringBuffer();

		for (int i = 0; i < parts.length - 1; i++) {
			s.append(parts[i]);
			s.append(".");
		}
		s.append(parts[parts.length - 1]);
		return s.toString();
	}

	private String[] append(String s, String[] a) {
		String[] result = new String[a.length + 1];

		for (int i = 0; i < a.length; i++) {
			result[i] = a[i];
		}
		result[a.length] = s;
		return result;
	}

	/**
	 * Appends random feature metadata to the given {@link Identity}.
	 * @param node
	 */
	private void appendFeatures(Identity node) {
		int featureCount = new Random().nextInt(mMaxFeaturePerCategory) + 1;

		for (int i = 0; i < featureCount; i++) {
			PublishUpdate update = Requests.createPublishUpdate();

			Document feature = createFeature(Math.random() + "");

			update.setIdentifier1(node);
			update.addMetadata(feature);
			update.setLifeTime(MetadataLifetime.forever);

			mPublishUpdates.add(update);
		}
	}

	private Document createCategoryLink(String name) {
		Document doc = mDocumentBuilder.newDocument();
		Element e = doc.createElementNS(NAMESPACE, NAMESPACE_PREFIX + ":" + name);
		e.setAttributeNS(null, "ifmap-cardinality", "singleValue");

		doc.appendChild(e);
		return doc;
	}

	private Document createFeature(String id) {
		Document doc = mDocumentBuilder.newDocument();
		Element feature = doc.createElementNS(NAMESPACE, NAMESPACE_PREFIX + ":feature");

		feature.setAttributeNS(null, "ifmap-cardinality", "multiValue");
		feature.setAttribute("ctxp-timestamp", "2012-01-01T22:22:22");
		feature.setAttribute("ctxp-position", "TODO");
		feature.setAttribute("ctxp-other-devices", "TODO");

		Element idElement = doc.createElement("id");
		idElement.setTextContent(id);
		feature.appendChild(idElement);

		Element typeElement = doc.createElement("type");
		typeElement.setTextContent("quantitive");
		feature.appendChild(typeElement);

		double v = mFeatureRandom.nextDouble() * 10;
		mFeatureValues.add(v);

		Element valueElement = doc.createElement("value");
		valueElement.setTextContent(v + "");
		feature.appendChild(valueElement);

		doc.appendChild(feature);
		return doc;
	}

	private void printStats() {
		double sum = 0;
		for (Double d : mFeatureValues) {
			sum += d;
		}

		Collections.sort(mFeatureValues);

		double median = ( mFeatureValues.size() % 2 != 0 ) ? // not even number of elements?
					( mFeatureValues.get(mFeatureValues.size() / 2) ) : // choose the middel
					((mFeatureValues.get((mFeatureValues.size() / 2) - 1) + // choose mean of both middle values
							mFeatureValues.get(mFeatureValues.size() / 2)) / 2);


		System.out.println("Number of features: " + mFeatureValues.size());
		System.out.println("Average: " + sum / mFeatureValues.size());
		System.out.println("Median: " + median);

//		System.out.println(mFeatureValues);
	}

	private Identity createCategory(String name, String admDomain) {
		return Identifiers.createIdentity(
				IdentityType.other,
				name,
				admDomain,
				OTHER_TYPE_DEFINITION);
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

		mHelpOp = new Option("h", "help", false, "print this message");
		mOptions.addOption(mHelpOp);


		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("device-identifier");
		OptionBuilder.withType(String.class);
		OptionBuilder.withDescription("the target device identifier");
		mDeviceIdentifierOption = OptionBuilder.create("i");
		mOptions.addOption(mDeviceIdentifierOption);

		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("tree-depth");
		OptionBuilder.withType(Integer.class);
		OptionBuilder.withDescription("depth of the feature tree");
		mTreeDepthOption = OptionBuilder.create("d");
		mOptions.addOption(mTreeDepthOption);

		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("max-childs-per-category");
		OptionBuilder.withType(Integer.class);
		OptionBuilder.withDescription("maximum childs per category");
		mMaxChildsPerCategoryOption = OptionBuilder.create("c");
		mOptions.addOption(mMaxChildsPerCategoryOption);

		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("max-feature-per-category");
		OptionBuilder.withType(Integer.class);
		OptionBuilder.withDescription("maximum features per category");
		mMaxFeaturePerCategoryOption = OptionBuilder.create("f");
		mOptions.addOption(mMaxFeaturePerCategoryOption);

		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			mCmdLine = parser.parse(mOptions, args);

			mDeviceIdentifier =
					mCmdLine.getOptionValue(mDeviceIdentifierOption.getOpt());
			mTreeDepth =
					new Integer(mCmdLine.getOptionValue(mTreeDepthOption.getOpt())).intValue();
			mMaxChildsPerCategory =
					new Integer(mCmdLine.getOptionValue(mMaxChildsPerCategoryOption.getOpt())).intValue();
			mMaxFeaturePerCategory =
					new Integer(mCmdLine.getOptionValue(mMaxFeaturePerCategoryOption.getOpt())).intValue();
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp(Feature.CMD, mOptions);
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

	public static void main(String[] args) {
		System.out.println("feature");

		try {
			Feature p = new Feature(args);
			p.start();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IfmapErrorResult e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IfmapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EndSessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
