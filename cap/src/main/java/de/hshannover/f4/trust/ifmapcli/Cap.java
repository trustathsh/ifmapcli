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

import javax.net.ssl.TrustManager;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.Config;
import de.hshannover.f4.trust.ifmapj.IfmapJ;
import de.hshannover.f4.trust.ifmapj.IfmapJHelper;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.IfmapErrorResult;
import de.hshannover.f4.trust.ifmapj.exception.IfmapException;
import de.hshannover.f4.trust.ifmapj.exception.InitializationException;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

/**
 * A simple tool that publishes or deletes capability metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author ib
 *
 */
public class Cap {
	final static String CMD = "cap";
	final static int MIN_ARGS = 3;			// update|delete, ar, cap
	final static int EXPECTED_ARGS = 8;		// update|delete, ar, cap
											// url, user, pass,
											// keystorePath, keystorePass

	// in order to create the necessary objects, make use of the appropriate
	// factory classes
	private static StandardIfmapMetadataFactory mf = IfmapJ
			.createStandardMetadataFactory();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String op, ar, cap;
		Config cfg;
		SSRC ssrc;
		PublishRequest req;
		PublishUpdate publishUpdate;
		PublishDelete publishDelete;
		TrustManager[] tms;
		Identifier arIdentifier;
		Document metadata;
		InputStream is;

		// check number of mandatory command line arguments
		if(args.length < MIN_ARGS){
			Cap.usage();
			return;
		}

		// parse mandatory command line arguments
		op = args[0];
		ar = args[1];
		cap = args[2];
		if(Common.isUpdateorDelete(op) == false){
			Cap.usage();
			return;
		}

		// check and load optional parameters
		cfg = Common.checkAndLoadParams(args, EXPECTED_ARGS);
		System.out.println(CMD + " uses config " + cfg);

		// prepare identifiers
		arIdentifier = Identifiers.createAr(ar);

		// prepare metadata
		metadata = mf.createCapability(cap);

		// update or delete
		if(Common.isUpdate(op)){
			publishUpdate = Requests.createPublishUpdate(arIdentifier,
					metadata, MetadataLifetime.forever);
			req = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "meta:capability[name='" + cap + "']";
			publishDelete = Requests.createPublishDelete(arIdentifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			req = Requests.createPublishReq(publishDelete);
		}

		// publish
		try {
			is = Common.prepareTruststoreIs(cfg.getTruststorePath());
			tms = IfmapJHelper.getTrustManagers(is, cfg.getTruststorePass());
			ssrc = IfmapJ.createSSRC(cfg.getUrl(), cfg.getUser(), cfg.getPass(), tms);
			ssrc.newSession();
			ssrc.publish(req);
			ssrc.endSession();
		} catch (InitializationException e) {
			System.out.println(e.getDescription() + " " + e.getMessage());
		} catch (IfmapErrorResult e) {
			System.out.println(e.getErrorString());
		} catch (IfmapException e) {
			System.out.println(e.getDescription() + " " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void usage() {
		System.out.println("usage:\n" +
				"\t" + Cap.CMD + " update|delete ar cap " +
				"[url user pass truststore truststorePass]");
		System.out.println(Common.USAGE);
	}
}

