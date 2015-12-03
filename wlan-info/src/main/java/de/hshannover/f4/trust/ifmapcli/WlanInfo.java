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
 * This file is part of ifmapcli (wlan-info), version 0.3.1, implemented by the Trust@HsH
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

import java.util.List;

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.IfmapjEnumConverter;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.WlanSecurityEnum;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityType;

/**
 * A simple tool that publishes or deletes wlan-information metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author bhl
 *
 */
public class WlanInfo extends AbstractClient {

	public static void main(String[] args) {
		command = "wlan-info";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addAccessRequest(parser);
		ParserUtil.addDevice(parser);
		ParserUtil.addWlanInfoSsid(parser);
		ParserUtil.addWlanInfoUnicastSecurity(parser);
		ParserUtil.addWlanInfoGroupSecurity(parser);
		ParserUtil.addWlanInfoManagementSecurity(parser);
		
		parseParameters(parser, args);
		
		printParameters(KEY_OPERATION, new String[] {KEY_ACCESS_REQUEST, 
				KEY_DEVICE,
				KEY_WLAN_INFORMATION_SSID,
				KEY_WLAN_INFORMATION_UNICAST_SECURITY,
				KEY_WLAN_INFORMATION_GROUP_SECURITY,
				KEY_WLAN_INFORMATION_MANAGEMENT_SECURITY});
		
		String ar = resource.getString(KEY_ACCESS_REQUEST);
		String device = resource.getString(KEY_DEVICE);
		String ssid = resource.getString(KEY_WLAN_INFORMATION_SSID);
		List<WlanSecurityEnum> ssidUnicastSecurity = resource.get(KEY_WLAN_INFORMATION_UNICAST_SECURITY);
		WlanSecurityEnum ssidGroupSecurity = resource.get(KEY_WLAN_INFORMATION_GROUP_SECURITY);
		List<WlanSecurityEnum> ssidManagementSecurity = resource.get(KEY_WLAN_INFORMATION_MANAGEMENT_SECURITY);
		
		// prepare identifiers
		Identifier arIdentifier = Identifiers.createAr(ar);
		Identifier devIdentifier = Identifiers.createDev(device);

		// prepare metadata
		Document metadata = mf.createWlanInformation(ssid,
				IfmapjEnumConverter.ifmapjWlanSecurityTypeListFrom(ssidUnicastSecurity),
				new WlanSecurityType(IfmapjEnumConverter.ifmapjWlanSecurityTypeFrom(ssidGroupSecurity), null),
				IfmapjEnumConverter.ifmapjWlanSecurityTypeListFrom(ssidManagementSecurity));

		PublishRequest request;
		// update or delete
		if (isUpdate(KEY_OPERATION)) {
			PublishUpdate publishUpdate = Requests.createPublishUpdate(arIdentifier, devIdentifier,
					metadata, MetadataLifetime.forever);
			request = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "meta:wlan-information";
			PublishDelete publishDelete = Requests.createPublishDelete(arIdentifier, devIdentifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			request = Requests.createPublishReq(publishDelete);
		}

		// publish
		publishIfmapData(request);
	}
}
