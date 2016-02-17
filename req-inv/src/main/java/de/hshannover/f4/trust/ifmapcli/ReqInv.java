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
 * This file is part of ifmapcli (req-inv), version 0.3.2, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2016 Trust@HsH
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

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple tool that publishes or deletes device-ip metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author rosso
 *
 */
public class ReqInv extends AbstractClient {

	public static void main(String[] args) {
		command = "req-inv";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addDevice(parser);
		ParserUtil.addOtherIdentifierType(parser, IdType.ipv4, IdType.ipv6, IdType.mac);
		ParserUtil.addOtherIdentifier(parser);
		ParserUtil.addQualifier(parser);

		parseParameters(parser, args);
		
		printParameters(KEY_OPERATION, new String[] {KEY_DEVICE, KEY_OTHER_IDENTIFIER_TYPE, KEY_OTHER_IDENTIFIER, KEY_QUALIFIER});
		
		Identifier deviceIdentifier = Identifiers.createDev(resource.getString(KEY_DEVICE));
		IdType otherIdentifierType = resource.get(KEY_OTHER_IDENTIFIER_TYPE);
		Identifier otherIdentifier = getIdentifier(
				otherIdentifierType,
				resource.getString(KEY_OTHER_IDENTIFIER));
		String qualifier = (resource.getString(KEY_QUALIFIER) == null) ? "" : resource.getString(KEY_QUALIFIER);
		Document metadata = mf.createRequestForInvestigation(qualifier);

		try {
			SSRC ssrc = createSSRC();
			ssrc.newSession();

			PublishRequest req = Requests.createPublishReq();
		
			if (isUpdate(KEY_OPERATION)) {
				PublishUpdate publishUpdate = Requests.createPublishUpdate(
						deviceIdentifier, otherIdentifier, metadata, MetadataLifetime.forever);
				req.addPublishElement(publishUpdate);
			} else if (isDelete(KEY_OPERATION)) {
				String filter = String.format(
					"meta:request-for-investigation[@ifmap-publisher-id='%s' and @qualifier='%s']",
					ssrc.getPublisherId(), qualifier);
				PublishDelete publishDelete = Requests.createPublishDelete(
						deviceIdentifier, otherIdentifier, filter);
				publishDelete.addNamespaceDeclaration("meta", IfmapStrings.STD_METADATA_NS_URI);
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
