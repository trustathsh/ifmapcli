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
 * This file is part of ifmapcli (perf1), version 0.0.6, implemented by the Trust@HsH
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

import java.io.InputStream;
import java.util.ArrayList;

import javax.net.ssl.TrustManager;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapj.IfmapJ;
import de.hshannover.f4.trust.ifmapj.IfmapJHelper;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Device;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.identifier.Identity;
import de.hshannover.f4.trust.ifmapj.identifier.IdentityType;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

/**
 * This is a class to test performance of an IF-MAP 2.0 server. It was created
 * based on input from Juniper Networks. The goal is to measure performance of a
 * MAPS when a large number of publish and subscribe operations are done by one
 * client. The test setup is as follows:
 *
 * - one IF-MAP client - send one subscribe message for the parent
 * identifier/node with max-depth of 1 - next, send many (thousands?) publish
 * messages - each publish will contain "update" elements to create a link with
 * the parent - lastly, do a poll
 *
 * @author ib
 *
 */
public class Perf1 {

	private final static String CMD = "perf1";
	private static int counter = 0;

	private static StandardIfmapMetadataFactory metaFac = IfmapJ
			.createStandardMetadataFactory();

	public static void main(String[] args) {
		
		long maxBytes = Runtime.getRuntime().maxMemory();
		System.out.println("Max memory: " + maxBytes / 1024 / 1024 + "M");
		
		final String KEY_NUMBER_REQUESTS = "requests";
		final String KEY_NUMBER_UPDATES = "updates";
		final String KEY_NUMBER_SPRINTS = "sprint-size";

		ArgumentParser parser = ArgumentParsers.newArgumentParser(CMD);
		parser.addArgument("requests")
			.type(Integer.class)
			.dest(KEY_NUMBER_REQUESTS)
			.help("number of publish requests");
		parser.addArgument("updates")
			.type(Integer.class)
			.dest(KEY_NUMBER_UPDATES)
			.help("number of update elements per request");
		parser.addArgument("sprint-size")
			.type(Integer.class)
			.dest(KEY_NUMBER_SPRINTS)
			.help("size of one sprint");
		ParserUtil.addConnectionArgumentsTo(parser);
		ParserUtil.addCommonArgumentsTo(parser);

		Namespace res = null;
		try {
			res = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		if (res.getBoolean(ParserUtil.VERBOSE)) {
			StringBuilder sb = new StringBuilder();
			
			sb.append(CMD).append(" ");
			ParserUtil.appendIntegerIfNotNull(sb, res, KEY_NUMBER_REQUESTS);
			ParserUtil.appendIntegerIfNotNull(sb, res, KEY_NUMBER_UPDATES);
			ParserUtil.appendIntegerIfNotNull(sb, res, KEY_NUMBER_SPRINTS);
			
			ParserUtil.printConnectionArguments(sb, res);
			System.out.println(sb.toString());
		}

		int numberRequests = res.getInt(KEY_NUMBER_REQUESTS);
		int numberUpdates = res.getInt(KEY_NUMBER_UPDATES);
		int sizeSprint = res.getInt(KEY_NUMBER_SPRINTS);
		
		int numberOfSprints;
		if (sizeSprint > numberRequests){
			// there is only one sprint
			numberOfSprints = 1;
			sizeSprint = numberRequests;
		} else {
			numberOfSprints = numberRequests / sizeSprint;
		}
		
		PublishRequest pr;
		PublishUpdate pu;
		Identity id;
		
		Device rootNode = Identifiers.createDev("parentNode");
		Document authBy = metaFac.createAuthBy();
		ArrayList<PublishRequest> publishRequests = new ArrayList<PublishRequest>(1000);

		// create a certain number of publish requests
		for (int i = 0; i < numberRequests; i++) {
			pr = Requests.createPublishReq();
			// create a certain number of publish updates
			for (int j = 0; j < numberUpdates; j++) {
				pu = Requests.createPublishUpdate();
				// generate new Identifier
				id = Identifiers.createIdentity(IdentityType.userName,
						new Integer(Perf1.counter++).toString());
				pu.setIdentifier1(rootNode);
				pu.setIdentifier2(id);
				pu.addMetadata(authBy);
				pr.addPublishElement(pu);
			}
			publishRequests.add(pr);
		}
		
		InputStream is;
		try {
			is = Common.prepareTruststoreIs(res.getString(ParserUtil.KEYSTORE_PATH));
			TrustManager[] tms = IfmapJHelper.getTrustManagers(is, res.getString(ParserUtil.KEYSTORE_PASS));
			SSRC ssrc = IfmapJ.createSSRC(
					res.getString(ParserUtil.URL),
					res.getString(ParserUtil.USER),
					res.getString(ParserUtil.PASS),
					tms);
			ssrc.newSession();

			long start = System.currentTimeMillis();

			for (int i = 0; i < numberOfSprints; i++) {
				System.out.print("Do publish sprint " + i);
				long startSprint = System.currentTimeMillis();
				for (int j = i * sizeSprint; j < (i * sizeSprint) + sizeSprint; j++) {
					pr = publishRequests.get(j);
					ssrc.publish(pr);
				}
				long endSprint = System.currentTimeMillis();
				System.out.print(" done! -> ");
				System.out.println("Duration: " + (endSprint - startSprint) + "ms");
			}

			long end = System.currentTimeMillis();
			System.out.println("Total Duration: " + (end - start) + "ms");

			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
