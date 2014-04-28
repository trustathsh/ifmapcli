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
 * This file is part of ifmapcli (ex-meta), version 0.1.0, implemented by the Trust@HsH
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

import org.w3c.dom.Document;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import de.hshannover.f4.trust.ifmapj.metadata.Cardinality;
import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple tool that publishes or deletes extended-metadata. When metadata is
 * published, the lifetime is set to be 'forever'.
 *
 * @author Marius Rohde
 *
 */

public class ExMeta extends AbstractClient {

	/**
	 * Method to start this module
	 *
	 */

	public static void main(String[] args) {
		command = "ex-meta";

		// ---parsing-------
		ArgumentParser parser = createDefaultParser();

		ParserUtil.addPublishOperation(parser);

		ParserUtil.addIdentifierType(parser, IdType.ipv4, IdType.ipv6, IdType.mac, IdType.dev, IdType.ar, IdType.id);
		ParserUtil.addIdentifier(parser);

		ParserUtil.addSecIdentifierType(parser, IdType.ipv4, IdType.ipv6, IdType.mac, IdType.dev, IdType.ar, IdType.id);
		ParserUtil.addSecIdentifier(parser);

		ParserUtil.addElementName(parser);
		ParserUtil.addCardinality(parser);
		ParserUtil.addAttributeName(parser);
		ParserUtil.addAttributeValue(parser);

		ParserUtil.addNamespacePrefix(parser);
		ParserUtil.addNamespaceUri(parser);

		parseParameters(parser, args);

		// printParameters(KEY_OPERATION, new String[] { KEY_IDENTIFIER_TYPE,
		// KEY_IDENTIFIER, KEY_OTHER_IDENTIFIER_TYPE,
		// KEY_OTHER_IDENTIFIER });

		// ---get parameters-------

		String nsPrefix = resource.getString(KEY_NAMESPACE_PREFIX);
		String nsUri = resource.getString(KEY_NAMESPACE_PREFIX);
		String attrName = resource.getString(KEY_ATTRIBUTE_NAME);
		String attrValue = resource.getString(KEY_NAMESPACE_PREFIX);

		if (nsPrefix == null) {
			nsPrefix = IfmapStrings.STD_METADATA_PREFIX;
		}
		if (nsUri == null) {
			nsUri = IfmapStrings.STD_METADATA_NS_URI;
		}

		IdType identifierType1 = resource.get(KEY_IDENTIFIER_TYPE);
		Identifier identifier1 = getIdentifier(identifierType1, resource.getString(KEY_IDENTIFIER));

		Document metadata;
		if (resource.getString(KEY_CARDINALITY).equals("singleValue")) {
			if (attrName == null || attrValue == null) {
				metadata = mf.create(resource.getString(KEY_ELEMENT_NAME), nsPrefix, nsUri, Cardinality.singleValue);
			} else {
				metadata = mf.create(resource.getString(KEY_ELEMENT_NAME), nsPrefix, nsUri, Cardinality.singleValue,
						attrName, attrValue);
			}
		} else {
			if (attrName == null || attrValue == null) {
				metadata = mf.create(resource.getString(KEY_ELEMENT_NAME), nsPrefix, nsUri, Cardinality.multiValue);
			} else {
				metadata = mf.create(resource.getString(KEY_ELEMENT_NAME), nsPrefix, nsUri, Cardinality.multiValue,
						attrName, attrValue);
			}
		}

		IdType identifierType2 = resource.get(KEY_SEC_IDENTIFIER_TYPE);
		String identifierName = resource.getString(KEY_SEC_IDENTIFIER);
		Identifier identifier2 = null;

		if (identifierType2 == null && identifierName != null) {
			throw new RuntimeException("no identifier type specified for given identifier: " + identifierName);
		} else if (identifierType2 != null && identifierName == null){
			throw new RuntimeException("no identifier specified for given identifier type: " + identifierType2);
		} else if (identifierType2 != null && identifierName != null) {
			identifier2 = getIdentifier(identifierType2, identifierName);
		}

		try {
			SSRC ssrc = createSSRC();
			ssrc.newSession();

			PublishRequest request;

			if (isUpdate(KEY_OPERATION)) {
				if (identifier2 == null) {
					PublishUpdate publishUpdate = Requests.createPublishUpdate(identifier1, metadata,
							MetadataLifetime.forever);
					request = Requests.createPublishReq(publishUpdate);
				} else {
					PublishUpdate publishUpdate = Requests.createPublishUpdate(identifier1, identifier2, metadata,
							MetadataLifetime.forever);
					request = Requests.createPublishReq(publishUpdate);
				}
			} else {
				PublishDelete publishDelete;
				String filter = String.format(nsPrefix + ":" + resource.getString(KEY_ELEMENT_NAME)
						+ "[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
				if (identifier2 == null) {
					publishDelete = Requests.createPublishDelete(identifier1, filter);
				} else {
					publishDelete = Requests.createPublishDelete(identifier1, identifier2, filter);
				}
				publishDelete.addNamespaceDeclaration(nsPrefix, nsUri);
				request = Requests.createPublishReq(publishDelete);
			}

			ssrc.publish(request);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

}
