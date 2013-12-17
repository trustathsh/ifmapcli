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
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
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
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishElement;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * This tool enables to publish and delete atomic feature and category
 * metadata. Lifetime is set to forever.
 *
 * Context Parameters are:
 * - ctx-param-time
 * - ctx-param-location
 * - ctx-param-other-devices
 *
 * If not specified, default values are used.
 *
 * subcategory-of metadata is not deleted by default but can be turned on
 *
 * @author ib
 *
 */
public class FeatureSingle {

	final static String CMD = "featureSingle";

	final static String OTHER_TYPE_DEFINITION = "32939:category";
	final static String NAMESPACE = "http://www.esukom.de/2012/ifmap-metadata/1";
	final static String NAMESPACE_PREFIX = "esukom";

	private SSRC mSsrc;

	private String mFullQualifiedInstanceAwareFeatureId;
	private FeatureType mType;
	private String mValue;
	private String mDevice;
	private boolean mIsUpdate;
	private String mCtxTime;
	private String mCtxPos;
	private String mCtxOtherDevices;
	private boolean mDeleteSubCatMetadata;


	private List<PublishElement> mPublishElements = new ArrayList<PublishElement>();

	private DocumentBuilderFactory mDocumentBuilderFactory;
	private DocumentBuilder mDocumentBuilder;

	// CLI options parser stuff ( not the actual input params )
	Options mOptions;

	Option mFullQualifiedInstanceAwareFeatureIdOp;
	Option mTypeOp;
	Option mValueOp;
	Option mIsUpdateOp;
	Option mDeviceOp;
	Option mCtxTimeOp;
	Option mCtxPosOp;
	Option mCtxOtherDevicesOp;
	Option mDeleteSubCateMetadataOp;

	Option mHelpOp;

	// parsed command line options
	CommandLine mCmdLine;

	// configuration
	Config mConfig;

	public FeatureSingle(String[] args) throws FileNotFoundException,
			InitializationException {
		mConfig = Common.loadEnvParams();
		System.out.println(CMD + " uses config " + mConfig);
		parseCommandLine(args);
		initSsrc();

		mDocumentBuilderFactory = DocumentBuilderFactory.newInstance();

		try {
			mDocumentBuilder = mDocumentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		preparePublishUpdatesOrDeletes();
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

		for (PublishElement el : mPublishElements) {
			req.addPublishElement(el);
		}

		mSsrc.publish(req);
		mSsrc.endSession();
	}

	private void preparePublishUpdatesOrDeletes() {
		PublishElement publishUpdateOrDelete;
		DummyCategory root = prepareCategoriesAndFeature();
		String nodeName = root.localId;
		Device dev = Identifiers.createDev(mDevice);
		Identity rootCategory = createCategory(nodeName);
		Document deviceCategory = createCategoryLink("device-category");

		if(mIsUpdate) {
			publishUpdateOrDelete = Requests.createPublishUpdate(dev, rootCategory, deviceCategory, MetadataLifetime.forever);
			mPublishElements.add(publishUpdateOrDelete);
		} else if (mDeleteSubCatMetadata) {
			publishUpdateOrDelete = Requests.createPublishDelete(dev, rootCategory);
			mPublishElements.add(publishUpdateOrDelete);
		}

		fork(rootCategory, root.features, root.subCategories);
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
		PublishElement pEl;

		// add features
		for (int i = 0; i < features.size(); i++) {
			DummyFeature df = features.get(i);
			Document meta = createFeature(df.localId, df.type.toString(), df.value);

			if(mIsUpdate) {
				pEl = Requests.createPublishUpdate(parent, meta, MetadataLifetime.forever);
			} else {
				String filter = makeDeleteFilter(df);
				PublishDelete publishDelete = Requests.createPublishDelete(parent, filter);
				publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
				publishDelete.addNamespaceDeclaration(NAMESPACE_PREFIX, NAMESPACE);
				pEl = publishDelete;
			}

			mPublishElements.add(pEl);
		}

		// add sub categories
		for (int i = 0; i < subCategories.size(); i++) {
			// get the category
			DummyCategory subCat = subCategories.get(i);

			// set the fqnn
			String fullNodeName = parent.getName() + "." + subCat.localId;

			// FIXME app:6:0
//			if(subCat.isMultiCard){
//				// check if this is the first instance
//				if(instanceCounter.get(fullNodeName) == null){
//					instanceCounter.put(fullNodeName, 0);
//				}
//				// attach current instance counter to name
//				int currentInstanceCounter = instanceCounter.get(fullNodeName);
//				// increase the counter
//				instanceCounter.put(fullNodeName, currentInstanceCounter + 1);
////				fullNodeName += ":" + currentInstanceCounter;
//			}

			Identity node = createCategory(fullNodeName);
			Document subCategoryOf = createCategoryLink("subcategory-of");

			if(mIsUpdate) {
				pEl = Requests.createPublishUpdate(parent,  node, subCategoryOf, MetadataLifetime.forever);
				mPublishElements.add(pEl);
			}  else if (mDeleteSubCatMetadata) {
				pEl = Requests.createPublishDelete(parent, node);
				mPublishElements.add(pEl);
			}

			fork(node, subCategories.get(i).features, subCategories.get(i).subCategories);
		}
	}

	private String makeDeleteFilter(DummyFeature df) {
		return NAMESPACE_PREFIX + ":feature[id='" + df.localId + "']";
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
		feature.setAttribute("ctxp-timestamp", mCtxTime);
		feature.setAttribute("ctxp-position", mCtxPos);
		feature.setAttribute("ctxp-other-devices", mCtxOtherDevices);

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

	private Identity createCategory(String name) {
		return Identifiers.createIdentity(
				IdentityType.other,
				name,
				mDevice,
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

		mDeleteSubCateMetadataOp = new Option("p", "purge subcategory-of", false, "delete subcategory-of metadata");
		mOptions.addOption(mDeleteSubCateMetadataOp);

		OptionBuilder.isRequired(false);
		OptionBuilder.withDescription("is update?");
		mIsUpdateOp = OptionBuilder.create("u");
		mOptions.addOption(mIsUpdateOp);

		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("fqiafi");
		OptionBuilder.withType(String.class);
		OptionBuilder.withDescription("full-qualified instance-aware feature id");
		mFullQualifiedInstanceAwareFeatureIdOp = OptionBuilder.create("i");
		mOptions.addOption(mFullQualifiedInstanceAwareFeatureIdOp);

		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("type");
		OptionBuilder.withType(FeatureType.class);
		OptionBuilder.withDescription("arbitrary|quantitive|qualified");
		mTypeOp = OptionBuilder.create("t");
		mOptions.addOption(mTypeOp);

		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("value");
		OptionBuilder.withType(String.class);
		OptionBuilder.withDescription("feature value");
		mValueOp = OptionBuilder.create("v");
		mOptions.addOption(mValueOp);

		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withArgName("dev");
		OptionBuilder.withType(String.class);
		OptionBuilder.withDescription("the device name (also used for adm domain in identity identifiers)");
		mDeviceOp = OptionBuilder.create("d");
		mOptions.addOption(mDeviceOp);

		OptionBuilder.hasArg();
		OptionBuilder.withArgName("ctxp-timestamp");
		OptionBuilder.withDescription("ctxp-timestamp");
		mCtxTimeOp = OptionBuilder.create("ctxt");
		mOptions.addOption(mCtxTimeOp);

		OptionBuilder.hasArg();
		OptionBuilder.withArgName("ctxp-position");
		OptionBuilder.withDescription("ctxp-position");
		mCtxPosOp = OptionBuilder.create("ctxp");
		mOptions.addOption(mCtxPosOp);

		OptionBuilder.hasArg();
		OptionBuilder.withArgName("ctxp-other-devices");
		OptionBuilder.withDescription("ctxp-other-devices");
		mCtxOtherDevicesOp = OptionBuilder.create("ctxo");
		mOptions.addOption(mCtxOtherDevicesOp);

		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			mCmdLine = parser.parse(mOptions, args);

			mFullQualifiedInstanceAwareFeatureId = mCmdLine.getOptionValue(mFullQualifiedInstanceAwareFeatureIdOp.getOpt());
			mType = Enum.valueOf(FeatureType.class, mCmdLine.getOptionValue(mTypeOp.getOpt()));
			mValue = mCmdLine.getOptionValue(mValueOp.getOpt());
			mIsUpdate = mCmdLine.hasOption(mIsUpdateOp.getOpt()) ? true : false;
			mDevice = mCmdLine.getOptionValue(mDeviceOp.getOpt());

			mCtxTime = mCmdLine.getOptionValue(mCtxTimeOp.getOpt(), Common.getTimeAsXsdDateTime(new Date()));
			mCtxPos = mCmdLine.getOptionValue(mCtxPosOp.getOpt(), "work");
			mCtxOtherDevices = mCmdLine.getOptionValue(mCtxOtherDevicesOp.getOpt(), "none");

			mDeleteSubCatMetadata = mCmdLine.hasOption(mDeleteSubCateMetadataOp.getOpt());

		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp(FeatureSingle.CMD, mOptions);
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
		System.out.println("FeatureSingle");

		try {
			FeatureSingle p = new FeatureSingle(args);
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
	private DummyCategory prepareCategoriesAndFeature(){
		DummyCategory root = null;
		DummyCategory node = null;
		DummyCategory parent = null;

		// parse fqiafi
		String delimiter = "\\.";
		String[] tokens = mFullQualifiedInstanceAwareFeatureId.split(delimiter);

		// set root
		root = new DummyCategory(tokens[0], tokens[0].contains(":") ? true : false);
		parent = root;
		node = root;

		// set the other categories
		for (int i = 1; i < tokens.length - 1; i++) {
			node = new DummyCategory(tokens[i], tokens[i].contains(":") ? true : false);
			parent.addSubCategory(node);
			parent = node;
		}

//		for (int i = 0; i < tokens.length; i++) {
//			System.out.println("### " + tokens[i]);
//		}
		// set the feature
		node.addDummyFeature(new DummyFeature(tokens[tokens.length-1], mType, mValue));

		return root;
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
		FeatureType type;
		String value;

		public DummyFeature(String localId, FeatureType type, String value) {
			this.localId = localId;
			this.type = type;
			this.value = value;
		}

	}

	private enum FeatureType {
		quantitive,
		qualified,
		arbitrary;

//		quantitive("quantitive"),
//		qualified("qualified"),
//		arbitrary("arbitrary");
//
//		private FeatureType(String name) {
//			this.name = name;
//		}
//
//		private final String name;
//
//		@Override
//		public String toString() {
//			return name;
//		}
	}
}
