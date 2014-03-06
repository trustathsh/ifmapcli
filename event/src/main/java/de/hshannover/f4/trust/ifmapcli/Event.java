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
 * This file is part of ifmapcli (event), version 0.1.0, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2014 Trust@HsH
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

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.IfmapjEnumConverter;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.EventType;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapcli.common.enums.Significance;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishNotify;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

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

	public static void main(String[] args) {
		command = "event";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperationWithNotify(parser);
		ParserUtil.addIdentifierType(parser, IdType.ipv4, IdType.ipv6, IdType.mac, IdType.dev, IdType.ar, IdType.id);
		ParserUtil.addIdentifier(parser);
		
		// event content
		ParserUtil.addEventName(parser);
		ParserUtil.addDiscovererId(parser);
		ParserUtil.addMagnitude(parser);
		ParserUtil.addConfidence(parser);
		ParserUtil.addSignificance(parser);
		ParserUtil.addEventType(parser);
		ParserUtil.addOtherTypeDefinition(parser);
		ParserUtil.addInformation(parser);
		ParserUtil.addEventVulnerabilityUri(parser);
		
		parseParameters(parser, args);

		printParameters(KEY_OPERATION, new String[] {KEY_IDENTIFIER_TYPE, KEY_IDENTIFIER, KEY_EVENT_NAME,
				KEY_DISCOVERER_ID, KEY_MAGNITUDE, KEY_CONFIDENCE, KEY_SIGNIFICANCE, KEY_EVENT_TYPE,
				KEY_OTHERTYPE_DEFINITION, KEY_INFORMATION, KEY_VULNERABILITY_URI});

		IdType identifierType = resource.get(KEY_IDENTIFIER_TYPE);
		String identifierName = resource.getString(KEY_IDENTIFIER);
		Identifier identifier = getIdentifier(identifierType, identifierName);

		EventType eventType = resource.get(KEY_EVENT_TYPE);
		Significance eventSignificance = resource.get(KEY_SIGNIFICANCE);
		Document event = mf.createEvent(
				resource.getString(KEY_EVENT_NAME),
				Common.getTimeAsXsdDateTime(new Date()), // TODO add
															// discovered-time
															// to argument
															// parser
				resource.getString(KEY_DISCOVERER_ID),
				resource.getInt(KEY_MAGNITUDE),
				resource.getInt(KEY_CONFIDENCE), 
				IfmapjEnumConverter.ifmapjSignificanceFrom(eventSignificance),
				IfmapjEnumConverter.ifmapjEventTypeFrom(eventType),
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
						ssrc.getPublisherId(), resource.getString(KEY_EVENT_NAME));

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
}
