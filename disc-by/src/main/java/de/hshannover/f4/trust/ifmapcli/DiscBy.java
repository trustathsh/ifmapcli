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
 * This file is part of ifmapcli (ip-disc-by), version 0.0.6, implemented by the Trust@HsH
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
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

/**
 * A simple tool that publishes or deletes discovered-by metadata on a link<br/>
 * between ip/mac and device identifiers.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author ib
 *
 */
public class DiscBy {
	enum IdType {
		ipv4, ipv6, mac
	}
	
	final static String CMD = "disc-by";

	// in order to create the necessary objects, make use of the appropriate
	// factory classes
	private static StandardIfmapMetadataFactory mf = IfmapJ
			.createStandardMetadataFactory();

	public static void main(String[] args) {
		final String KEY_OPERATION = "publishOperation";
		final String KEY_IDENTIFIER = "identifier";
		final String KEY_IDENTIFIER_TYPE = "identifierType";
		final String KEY_DEV = "device";

		ArgumentParser parser = ArgumentParsers.newArgumentParser(CMD);
		parser.addArgument("publish-operation")
			.type(String.class)
			.dest(KEY_OPERATION)
			.choices("update", "delete")
			.help("the publish operation");
		parser.addArgument("identifier-type")
		.type(IdType.class)
		.dest(KEY_IDENTIFIER_TYPE)
		.choices(
			IdType.ipv4,
			IdType.ipv6,
			IdType.mac)
		.help("the type of the identifier");
	parser.addArgument("identifier")
		.type(String.class)
		.dest(KEY_IDENTIFIER)
		.help("the identifier");
		parser.addArgument("device")
			.type(String.class)
			.dest(KEY_DEV)
			.help("name of the device identifier");
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
			sb.append(res.getString(KEY_OPERATION)).append(" ");
			sb.append(KEY_IDENTIFIER_TYPE).append("=").append(res.get(KEY_IDENTIFIER_TYPE)).append(" ");
			sb.append(KEY_IDENTIFIER).append("=").append(res.getString(KEY_IDENTIFIER)).append(" ");
			sb.append(KEY_DEV).append("=").append(res.getString(KEY_DEV)).append(" ");
			
			ParserUtil.printConnectionArguments(sb, res);
			System.out.println(sb.toString());
		}

		PublishRequest req;
		PublishUpdate publishUpdate;
		PublishDelete publishDelete;
		
		IdType identifierType = res.get(KEY_IDENTIFIER_TYPE);
		String identifierName = res.getString(KEY_IDENTIFIER);
		
		// prepare identifiers
		Identifier identifier = getIdentifier(identifierType, identifierName);
		Identifier devIdentifier = Identifiers.createDev(res.getString(KEY_DEV));

		// prepare metadata
		Document metadata = mf.createDiscoveredBy();

		// update or delete
		if (res.getString(KEY_OPERATION).equals("update")) {
			publishUpdate = Requests.createPublishUpdate(identifier, devIdentifier,
					metadata, MetadataLifetime.forever);
			req = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "meta:discovered-by";
			publishDelete = Requests.createPublishDelete(identifier, devIdentifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			req = Requests.createPublishReq(publishDelete);
		}

		// publish
		try {
			InputStream is = Common.prepareTruststoreIs(res.getString(ParserUtil.KEYSTORE_PATH));
			TrustManager[] tms = IfmapJHelper.getTrustManagers(is, res.getString(ParserUtil.KEYSTORE_PASS));
			SSRC ssrc = IfmapJ.createSSRC(
				res.getString(ParserUtil.URL),
				res.getString(ParserUtil.USER),
				res.getString(ParserUtil.PASS),
				tms);
			ssrc.newSession();
			ssrc.publish(req);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private static Identifier getIdentifier(IdType type, String name) {
		switch (type) {
		case ipv4:
			return Identifiers.createIp4(name);
		case ipv6:
			return Identifiers.createIp6(name);
		case mac:
			return Identifiers.createMac(name);
		default:
			throw new RuntimeException("unknown identifier type '" + type + "'");
		}
	}
}