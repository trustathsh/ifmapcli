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
 * This file is part of ifmapcli (feature), version 0.3.0, implemented by the Trust@HsH
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
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
public class Feature extends AbstractClient {

	final static String OTHER_TYPE_DEFINITION = "32939:category";
	final static String NAMESPACE = "http://www.esukom.de/2012/ifmap-metadata/1";
	final static String NAMESPACE_PREFIX = "esukom";

	private static String deviceIdentifier;
	private static int treeDepth;
	private static int maxChildsPerCategory;
	private static int maxFeaturePerCategory;

	private static List<PublishUpdate> publishUpdates = new ArrayList<PublishUpdate>();

	private static DocumentBuilder documentBuilder;

	private static List<Double> featureValues = new ArrayList<Double>();
	private static Random featureRandom = new Random(42L);

	/**
	 * Creates the child nodes for the given parent {@link Identity}. The
	 * resulting {@link PublishUpdate}s for each child are added to
	 * <code>mPublishUpdates</code>.
	 *
	 * @param parent
	 * @param currentDepth
	 */
	private static void fork(Identity parent, int currentDepth, String[] path) {
		if (currentDepth < treeDepth) {
			int childCount = new Random().nextInt(maxChildsPerCategory) + 1;

			for (int i = 0; i < childCount; i++) {
				PublishUpdate update = Requests.createPublishUpdate();

				String name = "category" + ((i % 4 == 0) ? ("X:" + (i / 4)) : i + "");
				String fullNodeName = joinStrings(path) + "." + name;
				Identity node = createCategory(fullNodeName, deviceIdentifier);

				Document subCategoryOf = createCategoryLink("subcategory-of");

				update.setIdentifier1(parent);
				update.setIdentifier2(node);
				update.addMetadata(subCategoryOf);
				update.setLifeTime(MetadataLifetime.forever);

				publishUpdates.add(update);
				appendFeatures(node);

				fork(node, currentDepth+1, append(name, path));
			}
		}
	}

	private static String joinStrings(String[] parts) {
		StringBuffer s = new StringBuffer();

		for (int i = 0; i < parts.length - 1; i++) {
			s.append(parts[i]);
			s.append(".");
		}
		s.append(parts[parts.length - 1]);
		return s.toString();
	}

	private static String[] append(String s, String[] a) {
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
	private static void appendFeatures(Identity node) {
		int featureCount = new Random().nextInt(maxFeaturePerCategory) + 1;

		for (int i = 0; i < featureCount; i++) {
			PublishUpdate update = Requests.createPublishUpdate();

			Document feature = createFeature(Math.random() + "");

			update.setIdentifier1(node);
			update.addMetadata(feature);
			update.setLifeTime(MetadataLifetime.forever);

			publishUpdates.add(update);
		}
	}

	private static Document createCategoryLink(String name) {
		Document doc = documentBuilder.newDocument();
		Element e = doc.createElementNS(NAMESPACE, NAMESPACE_PREFIX + ":" + name);
		e.setAttributeNS(null, "ifmap-cardinality", "singleValue");

		doc.appendChild(e);
		return doc;
	}

	private static Document createFeature(String id) {
		Document doc = documentBuilder.newDocument();
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

		double v = featureRandom.nextDouble() * 10;
		featureValues.add(v);

		Element valueElement = doc.createElement("value");
		valueElement.setTextContent(v + "");
		feature.appendChild(valueElement);

		doc.appendChild(feature);
		return doc;
	}

	private static void printStats() {
		double sum = 0;
		for (Double d : featureValues) {
			sum += d;
		}

		Collections.sort(featureValues);

		double median = (featureValues.size() % 2 != 0 ) ? // not even number of elements?
					(featureValues.get(featureValues.size() / 2) ) : // choose the middel
					((featureValues.get((featureValues.size() / 2) - 1) + // choose mean of both middle values
							featureValues.get(featureValues.size() / 2)) / 2);


		System.out.println("Number of features: " + featureValues.size());
		System.out.println("Average: " + sum / featureValues.size());
		System.out.println("Median: " + median);
	}

	private static Identity createCategory(String name, String admDomain) {
		return Identifiers.createIdentity(
				IdentityType.other,
				name,
				admDomain,
				OTHER_TYPE_DEFINITION);
	}

	public static void main(String[] args) {
		command = "feature";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addFeatureTargetDevice(parser);
		ParserUtil.addFeatureTreeDepth(parser);
		ParserUtil.addFeatureMaxChilds(parser);
		ParserUtil.addFeatureMaxFeatures(parser);

		parseParameters(parser, args);

		printParameters(new String[] {KEY_TARGET_DEVICE, KEY_DEPTH, KEY_MAX_CHILDS, KEY_MAX_FEATURES});
		
		deviceIdentifier = resource.getString(KEY_TARGET_DEVICE);
		treeDepth = resource.getInt(KEY_DEPTH);
		maxChildsPerCategory = resource.getInt(KEY_MAX_CHILDS);
		maxFeaturePerCategory = resource.getInt(KEY_MAX_FEATURES);
		
		try {
			SSRC ssrc = createSSRC();
	
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
	
			PublishUpdate update = Requests.createPublishUpdate();
	
			String nodeName = "rootCategory";
			Device dev = Identifiers.createDev(deviceIdentifier);
			Identity root = createCategory(nodeName, deviceIdentifier);
	
			Document deviceCategory = createCategoryLink("device-category");
	
			update.setIdentifier1(dev);
			update.setIdentifier2(root);
			update.addMetadata(deviceCategory);
			update.setLifeTime(MetadataLifetime.forever);
	
			publishUpdates.add(update);
	
			fork(root, 0, new String[] {nodeName});
			
			ssrc.newSession();
	
			PublishRequest req = Requests.createPublishReq();
	
			for (PublishUpdate u : publishUpdates) {
				req.addPublishElement(u);
			}
	
			ssrc.publish(req);
			ssrc.endSession();
	
			printStats();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
