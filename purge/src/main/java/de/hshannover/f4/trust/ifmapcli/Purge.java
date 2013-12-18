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
 * This file is part of ifmapcli (purge), version 0.0.6, implemented by the Trust@HsH
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

import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.Config;
import de.hshannover.f4.trust.ifmapj.IfmapJ;
import de.hshannover.f4.trust.ifmapj.IfmapJHelper;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.IfmapErrorResult;
import de.hshannover.f4.trust.ifmapj.exception.IfmapException;
import de.hshannover.f4.trust.ifmapj.exception.InitializationException;

/**
 * A simple tool that purges a publisher </br>.
 *
 * @author ib
 *
 */
public class Purge {
	final static String CMD = "purge";
	final static int MIN_ARGS = 1;			// me|<some-publisher-id>
	final static int EXPECTED_ARGS = 6;		// me|<some-publisher-id>
											// url, user, pass,
											// keystorePath, keystorePass

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String pId;
		Config cfg;
		SSRC ssrc;
		TrustManager[] tms;
		InputStream is;

		// check number of mandatory command line arguments
		if(args.length < MIN_ARGS){
			Purge.usage();
			return;
		}

		// parse mandatory command line arguments
		pId = args[0];

		// check and load optional parameters
		cfg = Common.checkAndLoadParams(args, EXPECTED_ARGS);
		System.out.println(CMD + " uses config " + cfg);

		// purge
		try {
			is = Common.prepareTruststoreIs(cfg.getTruststorePath());
			tms = IfmapJHelper.getTrustManagers(is, cfg.getTruststorePass());
			ssrc = IfmapJ.createSSRC(cfg.getUrl(), cfg.getUser(), cfg.getPass(), tms);
			ssrc.newSession();
			if(pId.equals("me")){
				ssrc.purgePublisher();
			} else {
				ssrc.purgePublisher(pId);
			}
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
				"\t" + Purge.CMD + " me|<some-publisher-id>");
		System.out.println(Common.USAGE);
	}
}
