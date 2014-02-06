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
 * This file is part of ifmapcli (event), version 0.0.6, implemented by the Trust@HsH
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

import java.util.Date;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishNotify;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.Significance;

/**
 * A simple tool that can publish event metadata.
 * 
 * Command line arguments specify the parameters.
 * 
 * Environment variables define the connection details of the MAPS.
 * 
 * @author ib
 * 
 */
public class Event extends AbstractClient {

	enum EventType {
		p2p, cve, botnet_infection, worm_infection, excessive_flows, behavioral_change, policy_violation, other
	}

	public static void main(String[] args) {
		command = "event";

		final String KEY_OPERATION = "publishOperation";
		final String KEY_IDENTIFIER = "identifier";
		final String KEY_IDENTIFIER_TYPE = "identifierType";
		final String KEY_NAME = "name";

		// TODO add discovered-time
		// final String KEY_DISCOVERED_TIME = "discovered-time";
		final String KEY_DISCOVERER_ID = "discoverer-id";
		final String KEY_MAGNITUDE = "magnitude";
		final String KEY_CONFIDENCE = "confidence";
		final String KEY_SIGNIFICANCE = "significance";
		final String KEY_TYPE = "type";
		final String KEY_OTHERTYPE_DEFINITION = "other-type-definition";
		final String KEY_INFORMATION = "information";
		final String KEY_VULNERABILITY_URI = "vulnerability-uri";

		ArgumentParser parser = createDefaultParser();
		parser.addArgument("publish-operation").type(String.class)
				.dest(KEY_OPERATION).choices("update", "delete", "notify")
				.help("the publish operation");
		parser.addArgument("identifier-type")
				.type(IdType.class)
				.dest(KEY_IDENTIFIER_TYPE)
				.choices(IdType.ipv4, IdType.ipv6, IdType.mac, IdType.dev,
						IdType.ar, IdType.id)
				.help("the type of the identifier");
		parser.addArgument("identifier").type(String.class)
				.dest(KEY_IDENTIFIER).help("the identifier");
		// event content
		parser.addArgument("name").type(String.class).dest(KEY_NAME)
				.help("the name of the event");

		// TODO add discovered-time
		// parser.addArgument(KEY_DISCOVERED_TIME)
		// .type(String.class)
		// .dest(KEY_DISCOVERED_TIME)
		// .setDefault(new Date())
		// .help("detection time of the event, default is now");
		parser.addArgument("--discoverer-id").type(String.class)
				.dest(KEY_DISCOVERER_ID).setDefault("ifmapj")
				.help("the discoverer-id of the event");
		parser.addArgument("--magnitude").type(Integer.class)
				.dest(KEY_MAGNITUDE).setDefault(0)
				.choices(Arguments.range(0, 100))
				.help("the magnitude of the event");
		parser.addArgument("--confidence").type(Integer.class)
				.dest(KEY_CONFIDENCE).setDefault(0)
				.choices(Arguments.range(0, 100))
				.help("the confidence for the event");
		parser.addArgument("--significance")
				.type(Significance.class)
				.setDefault(Significance.informational)
				.choices(Significance.critical, Significance.important,
						Significance.informational)
				.dest(KEY_SIGNIFICANCE)
				.help("the significance of the event");
		parser.addArgument("--type")
				.type(EventType.class)
				.choices(EventType.p2p, EventType.cve,
						EventType.botnet_infection, EventType.worm_infection,
						EventType.excessive_flows, EventType.behavioral_change,
						EventType.policy_violation, EventType.other)
				.dest(KEY_TYPE).setDefault(EventType.other)
				.help("the type of the event");
		parser.addArgument("--other-type-def").type(String.class)
				.dest(KEY_OTHERTYPE_DEFINITION)
				.help("other-type-definition of the event");
		parser.addArgument("--information").type(String.class)
				.dest(KEY_INFORMATION)
				.help("\"human consumable\" informational string");
		parser.addArgument("--vulnerability-uri").type(String.class)
				.dest(KEY_VULNERABILITY_URI)
				.help("URI of the CVE if type cve is used");

		parseParameters(parser, args);

		printParameters(KEY_OPERATION, new String[] {KEY_IDENTIFIER_TYPE, KEY_IDENTIFIER, KEY_NAME,
				KEY_DISCOVERER_ID, KEY_MAGNITUDE, KEY_CONFIDENCE, KEY_SIGNIFICANCE, KEY_TYPE,
				KEY_OTHERTYPE_DEFINITION, KEY_INFORMATION, KEY_VULNERABILITY_URI});

		IdType identifierType = resource.get(KEY_IDENTIFIER_TYPE);
		String identifierName = resource.getString(KEY_IDENTIFIER);
		Identifier identifier = getIdentifier(identifierType, identifierName);

		EventType eventType = resource.get(KEY_TYPE);
		Significance eventSignificance = resource.get(KEY_SIGNIFICANCE);
		Document event = mf.createEvent(
				resource.getString(KEY_NAME),
				Common.getTimeAsXsdDateTime(new Date()), // TODO add
															// discovered-time
															// to argument
															// parser
				resource.getString(KEY_DISCOVERER_ID),
				resource.getInt(KEY_MAGNITUDE),
				resource.getInt(KEY_CONFIDENCE), eventSignificance,
				ifmapjEventTypeFrom(eventType),
				resource.getString(KEY_OTHERTYPE_DEFINITION),
				resource.getString(KEY_INFORMATION),
				resource.getString(KEY_VULNERABILITY_URI));

		SSRC ssrc;
		try {
			ssrc = createSSRC();
			ssrc.newSession();

			PublishRequest req = Requests.createPublishReq();

			if (isUpdate(KEY_OPERATION)) {
				PublishUpdate publishUpdate = Requests.createPublishUpdate(
						identifier, event, MetadataLifetime.forever);
				req.addPublishElement(publishUpdate);
			} else if (isNotify(KEY_OPERATION)) {
				PublishNotify publishNotify = Requests.createPublishNotify(
						identifier, event);
				req.addPublishElement(publishNotify);
			} else if (isDelete(KEY_OPERATION)) {
				// TODO expand filter string to all event attributes
				String filter = String.format(
						"meta:event[@ifmap-publisher-id='%s' and name='%s']",
						ssrc.getPublisherId(), resource.getString(KEY_NAME));

				PublishDelete publishDelete = Requests.createPublishDelete(
						identifier, filter);
				publishDelete.addNamespaceDeclaration("meta",
						IfmapStrings.STD_METADATA_NS_URI);
				req.addPublishElement(publishDelete);
			}

			ssrc.publish(req);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	private static de.hshannover.f4.trust.ifmapj.metadata.EventType ifmapjEventTypeFrom(
			EventType type) {
		switch (type) {
		case p2p:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.p2p;
		case cve:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.cve;
		case botnet_infection:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.botnetInfection;
		case worm_infection:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.wormInfection;
		case excessive_flows:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.excessiveFlows;
		case behavioral_change:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.behavioralChange;
		case policy_violation:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.policyViolation;
		case other:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.other;
		default:
			throw new RuntimeException("unknown event type '" + type + "'");
		}
	}

}
