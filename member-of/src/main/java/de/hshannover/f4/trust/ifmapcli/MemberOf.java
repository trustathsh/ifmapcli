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
 * This file is part of ifmapcli (member-of), version 0.3.2, implemented by the Trust@HsH
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
import de.hshannover.f4.trust.ifmapj.extendedIdentifiers.IcsIdentifiers;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple tool that publishes or deletes member-of metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author pe
 *
 */
public class MemberOf extends AbstractClient {

	/**
	 * main method
	 */
	public static void main(String[] args) {
		command = "member-of";

		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addIdentifierType(parser, IdType.ics_ovNetwGr, IdType.ics_ovManagerGr);
		ParserUtil.addIdentifier(parser);
		ParserUtil.addIdentifierTypeTwo(parser, IdType.ics_bhi, IdType.id_dist);
		ParserUtil.addIdentifierTwo(parser);

		parseParameters(parser, args);

		printParameters(KEY_OPERATION, new String[] {KEY_IDENTIFIER_TYPE, KEY_IDENTIFIER, KEY_IDENTIFIER_TYPE_TWO, KEY_IDENTIFIER_TWO});

		IdType identifierType = resource.get(KEY_IDENTIFIER_TYPE);
		String identifierName = resource.getString(KEY_IDENTIFIER);
		IdType identifierTypeTwo = resource.get(KEY_IDENTIFIER_TYPE_TWO);
		String identifierNameTwo = resource.getString(KEY_IDENTIFIER_TWO);

		// prepare identifiers
		Identifier identifier = getIdentifier(identifierType, identifierName);
		Identifier identifierTwo = getIdentifier(identifierTypeTwo, identifierNameTwo);

		// prepare metadata
		Document metadata = icsmf.createMembOf();

		PublishRequest request;
		// update or delete
		if (isUpdate(KEY_OPERATION)) {
			PublishUpdate publishUpdate = Requests.createPublishUpdate(identifier, identifierTwo, metadata, MetadataLifetime.forever);
			request = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "ics-meta:member-of";
			PublishDelete publishDelete = Requests.createPublishDelete(identifier, identifierTwo, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.ICS_METADATA_PREFIX, IfmapStrings.ICS_METADATA_NS_URI);
			request = Requests.createPublishReq(publishDelete);
		}

		// publish
		publishIfmapData(request);
	}

}
