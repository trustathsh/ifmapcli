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
 * This file is part of ifmapcli (layer2-info), version 0.3.2, implemented by the Trust@HsH
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
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple tool that publishes or deletes layer2-information metadata on a link<br/>
 * between access-request and device identifiers.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author ib
 *
 */
public class Layer2Info extends AbstractClient {

	public static void main(String[] args) {
		command = "layer2-info";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addAccessRequest(parser);
		ParserUtil.addDevice(parser);
		ParserUtil.addVlanNumber(parser);
		ParserUtil.addVlanName(parser);
		ParserUtil.addPort(parser);
		ParserUtil.addAdministrativeDomain(parser);
		
		parseParameters(parser, args);
		
		printParameters(KEY_OPERATION, new String[] {KEY_ACCESS_REQUEST, KEY_DEVICE, KEY_VLAN_NUMBER, KEY_VLAN_NAME, KEY_PORT, KEY_ADMINISTRATIVE_DOMAIN});
	
		String ar = resource.getString(KEY_ACCESS_REQUEST);
		String dev = resource.getString(KEY_DEVICE);
		Integer vlanNumber = resource.getInt(KEY_VLAN_NUMBER);
		String vlanName = resource.getString(KEY_VLAN_NAME);
		Integer port = resource.getInt(KEY_PORT);
		String administrativeDomain = resource.getString(KEY_ADMINISTRATIVE_DOMAIN);
		
		// prepare identifiers
		Identifier arIdentifier = Identifiers.createAr(ar);
		Identifier devIdentifier = Identifiers.createDev(dev);

		Document metadata = null;
		// prepare metadata
		metadata = mf.createLayer2Information(
				vlanNumber,
				vlanName,
				port,
				administrativeDomain);

		PublishRequest request;
		// update or delete
		if (isUpdate(KEY_OPERATION)) {
			PublishUpdate publishUpdate = Requests.createPublishUpdate(arIdentifier, devIdentifier,
					metadata, MetadataLifetime.forever);
			request = Requests.createPublishReq(publishUpdate);
		} else {
			// TODO check if values for VLAN_NUMBER, VLAN_NAME and PORT are available
			String filter = "meta:layer2-information[vlan=" + vlanNumber + " " +
													"and vlan-name='" + vlanName + "' " +
													"and port=" + port +
													"]";
			PublishDelete publishDelete = Requests.createPublishDelete(arIdentifier, devIdentifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			request = Requests.createPublishReq(publishDelete);
		}

		// publish
		publishIfmapData(request);
	}
}
