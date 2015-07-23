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
 * This file is part of ifmapcli (location), version 0.3.0, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2015 Trust@HsH
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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.LocationInformation;

/**
 * A simple tool that publishes or deletes device-ip metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author ib
 *
 */
public class Location extends AbstractClient {

	public static void main(String[] args) {
		command = "location";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addIdentifierType(parser, IdType.id, IdType.ipv4, IdType.ipv6, IdType.mac);
		ParserUtil.addIdentifier(parser);
		ParserUtil.addLocationInfoTypes(parser);
		ParserUtil.addLocationInfoValues(parser);
		ParserUtil.addDiscoveredTime(parser);
		ParserUtil.addDiscovererId(parser);

		parseParameters(parser, args);
		
		printParameters(KEY_OPERATION, new String[] {KEY_IDENTIFIER_TYPE, KEY_IDENTIFIER, KEY_LOCATION_INFORMATION_TYPE, KEY_LOCATION_INFORMATION_VALUE, KEY_DISCOVERED_TIME, KEY_DISCOVERER_ID});
		
		IdType identifierType = resource.get(KEY_IDENTIFIER_TYPE);
		String identifierName = resource.getString(KEY_IDENTIFIER);
		Identifier identifier = getIdentifier(identifierType, identifierName);
		
		List<String> locationTypes = resource.get(KEY_LOCATION_INFORMATION_TYPE);
		List<String> locationValues = resource.get(KEY_LOCATION_INFORMATION_VALUE);
		List<LocationInformation> locationList = new ArrayList<LocationInformation>();
		
		for (int i = 0; i < locationTypes.size(); i++) {
			String type = locationTypes.get(i);
			if (i < locationValues.size()) {
				String value = locationValues.get(i);
				LocationInformation li = new LocationInformation(type, value);
				locationList.add(li);
			}
		}
			
		String discoveredTime = resource.getString(KEY_DISCOVERED_TIME);
		String discovererId = resource.getString(KEY_DISCOVERER_ID);
		
		// prepare metadata
		Document metadata = mf.createLocation(locationList, discoveredTime, discovererId);

		PublishRequest request;
		// update or delete
		if (isUpdate(KEY_OPERATION)) {
			PublishUpdate publishUpdate = Requests.createPublishUpdate(identifier, metadata, MetadataLifetime.forever);
			request = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "meta:location";
			PublishDelete publishDelete = Requests.createPublishDelete(identifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			request = Requests.createPublishReq(publishDelete);
		}

		// publish
		publishIfmapData(request);
	}
}
