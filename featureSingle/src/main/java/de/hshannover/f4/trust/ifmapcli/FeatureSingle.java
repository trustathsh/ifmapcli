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
 * This file is part of ifmapcli (featureSingle), version 0.3.1, implemented by the Trust@HsH
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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.FeatureType;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
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
public class FeatureSingle extends AbstractClient {

	final static String OTHER_TYPE_DEFINITION = "32939:category";
	final static String NAMESPACE = "http://www.esukom.de/2012/ifmap-metadata/1";
	final static String NAMESPACE_PREFIX = "esukom";

	private static String mFullQualifiedInstanceAwareFeatureId;
	private static FeatureType mType;
	private static String mValue;
	private static String mDevice;
	private static boolean mIsUpdate;
	private static String mCtxTime;
	private static String mCtxPos;
	private static String mCtxOtherDevices;

	private static List<PublishElement> mPublishElements = new ArrayList<PublishElement>();

	private static DocumentBuilderFactory mDocumentBuilderFactory;
	private static DocumentBuilder mDocumentBuilder;

	private static void preparePublishUpdatesOrDeletes() {
		PublishElement publishUpdateOrDelete;
		DummyCategory root = prepareCategoriesAndFeature();
		String nodeName = root.localId;
		Device dev = Identifiers.createDev(mDevice);
		Identity rootCategory = createCategory(nodeName);
		Document deviceCategory = createCategoryLink("device-category");

		if (mIsUpdate) {
			publishUpdateOrDelete = Requests.createPublishUpdate(dev, rootCategory, deviceCategory, MetadataLifetime.forever);
			mPublishElements.add(publishUpdateOrDelete);
		} else {
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
	private static void fork(Identity parent, Vector<DummyFeature> features, Vector<DummyCategory> subCategories) {

//		HashMap<String, Integer> instanceCounter = new HashMap<String, Integer>();
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
			}  else {
				pEl = Requests.createPublishDelete(parent, node);
				mPublishElements.add(pEl);
			}

			fork(node, subCategories.get(i).features, subCategories.get(i).subCategories);
		}
	}

	private static String makeDeleteFilter(DummyFeature df) {
		return NAMESPACE_PREFIX + ":feature[id='" + df.localId + "']";
	}

	private static Document createCategoryLink(String name) {
		Document doc = mDocumentBuilder.newDocument();
		Element e = doc.createElementNS(NAMESPACE, NAMESPACE_PREFIX + ":" + name);
		e.setAttributeNS(null, "ifmap-cardinality", "singleValue");

		doc.appendChild(e);
		return doc;
	}

	private static Document createFeature(String id, String type, String value) {
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

	private static Identity createCategory(String name) {
		return Identifiers.createIdentity(
				IdentityType.other,
				name,
				mDevice,
				OTHER_TYPE_DEFINITION);
	}

	public static void main(String[] args) {
		command = "featureSingle";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addFeatureId(parser);
		ParserUtil.addFeatureType(parser);
		ParserUtil.addFeatureValue(parser);
		ParserUtil.addFeatureTargetDevice(parser);
		ParserUtil.addFeatureContextTimestamp(parser);
		ParserUtil.addFeatureContextPosition(parser);
		ParserUtil.addFeatureContextOtherDevices(parser);
		
		parseParameters(parser, args);
		
		printParameters(KEY_OPERATION, new String[] {KEY_FEATURE_ID, KEY_FEATURE_TYPE, KEY_FEATURE_VALUE, KEY_TARGET_DEVICE, KEY_CTX_TIMESTAMP, KEY_CTX_POSITION, KEY_CTX_OTHER_DEVICES});

		mFullQualifiedInstanceAwareFeatureId = resource.getString(KEY_FEATURE_ID);
		mType = resource.get(KEY_FEATURE_TYPE);
		mValue = resource.getString(KEY_FEATURE_VALUE);
		mIsUpdate = resource.getString(KEY_OPERATION).equals("update") ? true : false;
		mDevice = resource.getString(KEY_TARGET_DEVICE);

		mCtxTime = resource.getString(KEY_CTX_TIMESTAMP);
		mCtxPos = resource.getString(KEY_CTX_POSITION);
		mCtxOtherDevices = resource.getString(KEY_CTX_OTHER_DEVICES);

		mDocumentBuilderFactory = DocumentBuilderFactory.newInstance();

		try {
			mDocumentBuilder = mDocumentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		preparePublishUpdatesOrDeletes();
		
		try {
			SSRC ssrc = createSSRC();
			ssrc.newSession();
			PublishRequest req = Requests.createPublishReq();
			
			for (PublishElement el : mPublishElements) {
				req.addPublishElement(el);
			}
			
			ssrc.publish(req);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static DummyCategory prepareCategoriesAndFeature(){
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

	private static class DummyCategory {
		String localId;
		Vector<DummyCategory> subCategories;
		Vector<DummyFeature> features;
//		boolean isMultiCard;

		DummyCategory(String localId, boolean isMultiCard) {
			this.localId = localId;
			this.subCategories = new Vector<DummyCategory>();
			this.features = new Vector<DummyFeature>();
//			this.isMultiCard = isMultiCard;
		}

		void addSubCategory(DummyCategory dc){
			subCategories.add(dc);
		}

		void addDummyFeature(DummyFeature df){
			features.add(df);
		}
	}

	private static class DummyFeature {
		String localId;
		FeatureType type;
		String value;

		public DummyFeature(String localId, FeatureType type, String value) {
			this.localId = localId;
			this.type = type;
			this.value = value;
		}
	}

}
