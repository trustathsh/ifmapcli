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
 * This file is part of ifmapcli (req-inv), version 0.0.6, implemented by the Trust@HsH
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
 * A simple tool that publishes or deletes device-ip metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author rosso
 *
 */
public class ReqInv {

	enum IdType {
		ipv4, ipv6, mac
	}

	public final static String CMD = "req-inv";

	// in order to create the necessary objects, make use of the appropriate
	// factory classes
	private static StandardIfmapMetadataFactory mf = IfmapJ
			.createStandardMetadataFactory();

	public static void main(String[] args) {
		final String KEY_OPERATION = "publishOperation";
		final String KEY_DEVICE = "device";
		final String KEY_OTHER_IDENTIFIER_TYPE = "other-identifier-type";
		final String KEY_OTHER_IDENTIFIER = "other-identifier";
		final String KEY_QUALIFIER = "qualifier";

		ArgumentParser parser = ArgumentParsers.newArgumentParser(CMD);
		parser.addArgument("publish-operation")
			.type(String.class)
			.dest(KEY_OPERATION)
			.choices("update", "delete")
			.help("the publish operation");
		parser.addArgument(KEY_DEVICE)
			.type(String.class)
			.dest(KEY_DEVICE)
			.help("the name of the device identifier");
		parser.addArgument(KEY_OTHER_IDENTIFIER_TYPE)
			.type(IdType.class)
			.dest(KEY_OTHER_IDENTIFIER_TYPE)
			.choices(IdType.ipv4, IdType.ipv6, IdType.mac)
			.help("the type of the other identifier");
		parser.addArgument(KEY_OTHER_IDENTIFIER)
			.type(String.class)
			.dest(KEY_OTHER_IDENTIFIER)
			.help("the name of the other identifier");
		parser.addArgument("--qualifier")
			.type(String.class)
			.dest(KEY_QUALIFIER)
			.help("the qualifier for the request-for-investigation");
		ParserUtil.addConnectionArgumentsTo(parser);
		ParserUtil.addCommonArgumentsTo(parser);

		Namespace res = null;
		try {
			res = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		Identifier deviceIdentifier = Identifiers.createDev(res.getString(KEY_DEVICE));
		IdType otherIdentifierType = res.get(KEY_OTHER_IDENTIFIER_TYPE);
		Identifier otherIdentifier = getIdentifier(
				otherIdentifierType,
				res.getString(KEY_OTHER_IDENTIFIER));
		String qualifier = (res.getString(KEY_QUALIFIER) == null) ? "" : res.getString(KEY_QUALIFIER);
		Document metadata = mf.createRequestForInvestigation(qualifier);

		SSRC ssrc = null;
		try {
			InputStream is = Common.prepareTruststoreIs(res.getString(ParserUtil.KEYSTORE_PATH));
			TrustManager[] tms = IfmapJHelper.getTrustManagers(is, res.getString(ParserUtil.KEYSTORE_PASS));
			ssrc = IfmapJ.createSSRC(
					res.getString(ParserUtil.URL),
					res.getString(ParserUtil.USER),
					res.getString(ParserUtil.PASS),
					tms);
			ssrc.newSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		PublishRequest req = Requests.createPublishReq();

		if (res.getString(KEY_OPERATION).equals("update")) {
			PublishUpdate publishUpdate = Requests.createPublishUpdate(
					deviceIdentifier, otherIdentifier, metadata, MetadataLifetime.forever);
			req.addPublishElement(publishUpdate);
		} else if (res.getString(KEY_OPERATION).equals("delete")) {
			String filter = String.format(
				"meta:request-for-investigation[@ifmap-publisher-id='%s' and @qualifier='%s']",
				ssrc.getPublisherId(), qualifier);
			PublishDelete publishDelete = Requests.createPublishDelete(
					deviceIdentifier, otherIdentifier, filter);
			publishDelete.addNamespaceDeclaration("meta", IfmapStrings.STD_METADATA_NS_URI);
			req.addPublishElement(publishDelete);
		}

		try {
			ssrc.publish(req);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static final Identifier getIdentifier(IdType type, String name) {
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
