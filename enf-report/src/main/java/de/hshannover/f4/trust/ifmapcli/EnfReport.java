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
 * This file is part of ifmapcli (enf-report), version 0.0.6, implemented by the Trust@HsH
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

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.IfmapjEnumConverter;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.EnforcementAction;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple tool that publishes or deletes enforcement-report metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author bhl
 *
 */
public class EnfReport extends AbstractClient {

	public static void main(String[] args) {
		command = "enf-report";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addDevice(parser);
		ParserUtil.addIdentifierType(parser, IdType.mac, IdType.ipv4, IdType.ipv6);
		ParserUtil.addIdentifier(parser);
		ParserUtil.addEnforcementAction(parser);
		ParserUtil.addOtherTypeDefinition(parser);
		ParserUtil.addEnforcementReason(parser);

		parseParameters(parser, args);
		
		printParameters(KEY_OPERATION, new String[] {KEY_DEVICE, KEY_IDENTIFIER_TYPE, KEY_IDENTIFIER, KEY_ENFORCEMENT_ACTION, KEY_OTHERTYPE_DEFINITION, KEY_ENFORCEMENT_REASON});
		
		String device = resource.getString(KEY_DEVICE);
		EnforcementAction enforcementAction = resource.get(KEY_ENFORCEMENT_ACTION);
		String otherTypeDefinition = resource.getString(KEY_OTHERTYPE_DEFINITION);
		String enforcementReason = resource.getString(KEY_ENFORCEMENT_REASON);
		
		// prepare identifiers
		Identifier devIdentifier = Identifiers.createDev(device);
		Identifier identifier = getIdentifier();

		// prepare metadata
		Document metadata = mf.createEnforcementReport(IfmapjEnumConverter.ifmapjEnforcementActionFrom(enforcementAction),
				otherTypeDefinition, enforcementReason);

		PublishRequest request;
		// update or delete
		if (isUpdate(KEY_OPERATION)) {
			PublishUpdate publishUpdate = Requests.createPublishUpdate(devIdentifier, identifier,
					metadata, MetadataLifetime.forever);
			request = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "meta:enforcement-report";
			PublishDelete publishDelete = Requests.createPublishDelete(devIdentifier, identifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			request = Requests.createPublishReq(publishDelete);
		}

		// publish
		publishIfmapData(request);
	}
}
