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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

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
 * A simple publisher implementation that publishes some specific
 * feature metadata for a smartphone as specified in ESUKOM WP4 for MalApp
 * Detection.
 *
 *
 * @author Ingo Bente
 *
 */
public class Feature2 {

	final static String CMD = "feature2";

	final static String[] PERMISSIONS = {	"ACCESS_COARSE_LOCATION",
		"ACCESS_NETWORK_STATE", "SEND_SMS", "INTERNET", "INSTALL_PACKAGES"};

	final static String OTHER_TYPE_DEFINITION = "32939:category";
	final static String NAMESPACE = "http://www.esukom.de/2012/ifmap-metadata/1";
	final static String NAMESPACE_PREFIX = "esukom";

	private SSRC mSsrc;

	private String mDeviceIdentifier;

	private List<PublishUpdate> mPublishUpdates = new ArrayList<PublishUpdate>();

	private DocumentBuilderFactory mDocumentBuilderFactory;
	private DocumentBuilder mDocumentBuilder;

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

	public Feature2(String[] args) throws FileNotFoundException,
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
	 * Create session, start publishing, end session
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
	}

	private void preparePublishUpdates() {
		PublishUpdate update = Requests.createPublishUpdate();

		DummyCategory root = prepareCategoriesAndFeatures();

		// from device to category
		String nodeName = root.localId;
		Device dev = Identifiers.createDev(mDeviceIdentifier);
		Identity rootCategory = createCategory(nodeName, mDeviceIdentifier);
		Document deviceCategory = createCategoryLink("device-category");
		update.setIdentifier1(dev);
		update.setIdentifier2(rootCategory);
		update.addMetadata(deviceCategory);
		update.setLifeTime(MetadataLifetime.forever);
		mPublishUpdates.add(update);

		fork(rootCategory, root.features, root.subCategories);

//		fork(smartphoneCategory, new String[] {nodeName}, );
	}

	/**
	 * Creates the child nodes for the given parent {@link Identity}. The
	 * resulting {@link PublishUpdate}s for each child are added to
	 * <code>mPublishUpdates</code>.
	 *
	 * @param parent
	 * @param currentDepth
	 */
	private void fork(Identity parent, Vector<DummyFeature> features, Vector<DummyCategory> subCategories) {

		HashMap<String, Integer> instanceCounter = new HashMap<String, Integer>();

		// add features
		for (int i = 0; i < features.size(); i++) {
			DummyFeature df = features.get(i);
			Document meta = createFeature(df.localId, df.type, df.value);

			PublishUpdate update = Requests.createPublishUpdate();
			update.setIdentifier1(parent);
			update.addMetadata(meta);
			update.setLifeTime(MetadataLifetime.forever);

			mPublishUpdates.add(update);
		}

		// add sub categories
		for (int i = 0; i < subCategories.size(); i++) {
			// get the category
			DummyCategory subCat = subCategories.get(i);

			// set the fqnn
			String fullNodeName = parent.getName() + "." + subCat.localId;

			if(subCat.isMultiCard){
				// check if this is the first instance
				if(instanceCounter.get(fullNodeName) == null){
					instanceCounter.put(fullNodeName, 0);
				}
				// attach current instance counter to name
				int currentInstanceCounter = instanceCounter.get(fullNodeName);
				// increase the counter
				instanceCounter.put(fullNodeName, currentInstanceCounter + 1);
				fullNodeName += ":" + currentInstanceCounter;
			}

			Identity node = createCategory(fullNodeName, mDeviceIdentifier);
			Document subCategoryOf = createCategoryLink("subcategory-of");

			PublishUpdate update = Requests.createPublishUpdate();
			update.setIdentifier1(parent);
			update.setIdentifier2(node);
			update.addMetadata(subCategoryOf);
			update.setLifeTime(MetadataLifetime.forever);

			mPublishUpdates.add(update);

			fork(node, subCategories.get(i).features, subCategories.get(i).subCategories);
		}
	}

	private Document createCategoryLink(String name) {
		Document doc = mDocumentBuilder.newDocument();
		Element e = doc.createElementNS(NAMESPACE, NAMESPACE_PREFIX + ":" + name);
		e.setAttributeNS(null, "ifmap-cardinality", "singleValue");

		doc.appendChild(e);
		return doc;
	}

	private Document createFeature(String id, String type, String value) {
		Document doc = mDocumentBuilder.newDocument();
		Element feature = doc.createElementNS(NAMESPACE, NAMESPACE_PREFIX + ":feature");

		feature.setAttributeNS(null, "ifmap-cardinality", "multiValue");
		feature.setAttribute("ctxp-timestamp", Common.getTimeAsXsdDateTime(new Date()));
		feature.setAttribute("ctxp-position", "TODO");
		feature.setAttribute("ctxp-other-devices", "TODO");

		Element idElement = doc.createElement("id");
		idElement.setTextContent(id);
		feature.appendChild(idElement);

		Element typeElement = doc.createElement("type");
		typeElement.setTextContent(type);
		feature.appendChild(typeElement);

		Element valueElement = doc.createElement("value");
		valueElement.setTextContent(value);
		feature.appendChild(valueElement);

		doc.appendChild(feature);
		return doc;
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

		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			mCmdLine = parser.parse(mOptions, args);

			mDeviceIdentifier =
					mCmdLine.getOptionValue(mDeviceIdentifierOption.getOpt());
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp(Feature2.CMD, mOptions);
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
		System.out.println("feature2");

		try {
			Feature2 p = new Feature2(args);
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

	/**
	 *
	 */
	private DummyCategory prepareCategoriesAndFeatures(){

		// root level categories
		DummyCategory smartphone = new DummyCategory("smartphone", false);

		// communication
		DummyCategory communication = new DummyCategory("communication", false);
		smartphone.addSubCategory(communication);
		// sms
		DummyCategory sms = new DummyCategory("sms", false);
		sms.addDummyFeature(new DummyFeature("SentCount", "quantitive", "23"));
		communication.addSubCategory(sms);

		// system
		DummyCategory system = new DummyCategory("system", false);
		system.addDummyFeature(new DummyFeature("RamFree", "quantitive", "85"));
		system.addDummyFeature(new DummyFeature("CpuLoad", "quantitive", "77"));
		system.addDummyFeature(new DummyFeature("ProcessCount", "quantitive", "1234"));
		system.addDummyFeature(new DummyFeature("BatteryLevel", "quantitive", "22"));
		smartphone.addSubCategory(system);

		// android
		DummyCategory android = new DummyCategory("android", false);
		smartphone.addSubCategory(android);

		// os
		DummyCategory os = new DummyCategory("os", false);
		os.addDummyFeature(new DummyFeature("KernelVersion", "arbitrary", "3.0.2"));
		os.addDummyFeature(new DummyFeature("FirmwareVersion", "arbitrary", "1.11.1"));
		os.addDummyFeature(new DummyFeature("BasebandVersion", "arbitrary", "I9250XXLA2"));
		os.addDummyFeature(new DummyFeature("BuildNumber", "arbitrary", "IMM76l"));
		os.addDummyFeature(new DummyFeature("Version", "arbitrary", "4.0.4"));
		android.addSubCategory(os);

		// apps
		for (int i = 0; i < 10; i++) {
			DummyCategory app = new DummyCategory("app", true);
			app.addDummyFeature(new DummyFeature("Name", "arbitrary", "MyCoolApp-" + i));
			app.addDummyFeature(new DummyFeature("Version", "quantitive", "3"));
			app.addDummyFeature(new DummyFeature("IsRunning", "qualified", i % 3 == 0 ? "true" : "false"));
			app.addDummyFeature(new DummyFeature("Author", "arbitrary", "Forname Lastname"));
			app.addDummyFeature(new DummyFeature("Rating", "quantitive", "4"));

			// permissions
			for (int j = 0; j < PERMISSIONS.length; j++) {
				DummyCategory perm = new DummyCategory("permission", true);
				perm.addDummyFeature(new DummyFeature("Name", "arbitrary", PERMISSIONS[j]));
				app.addSubCategory(perm);
			}
			android.addSubCategory(app);
		}

		return smartphone;
	}


	private class DummyCategory {
		String localId;
		Vector<DummyCategory> subCategories;
		Vector<DummyFeature> features;
		boolean isMultiCard;

		DummyCategory(String localId, boolean isMultiCard) {
			this.localId = localId;
			this.subCategories = new Vector<DummyCategory>();
			this.features = new Vector<DummyFeature>();
			this.isMultiCard = isMultiCard;
		}

		void addSubCategory(DummyCategory dc){
			subCategories.add(dc);
		}

		void addDummyFeature(DummyFeature df){
			features.add(df);
		}

	}

	private class DummyFeature {
		String localId;
		String type;
		String value;

		public DummyFeature(String localId, String type, String value) {
			this.localId = localId;
			this.type = type;
			this.value = value;
		}

	}

}
