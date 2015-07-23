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
 * This file is part of ifmapcli (bhi-addr), version 0.3.0, implemented by the Trust@HsH
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

import org.w3c.dom.Document;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.extendedIdentifiers.IcsIdentifiers;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.identifier.Identity;
import de.hshannover.f4.trust.ifmapj.identifier.IpAddress;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple tool that publishes or deletes bhi-address metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author pe
 *
 */
public class BhiAddr extends AbstractClient {

	/**
	 * main method
	 */
	public static void main(String[] args) {
		command = "bhi-addr";

		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addIcsBackhaulInterface(parser);
		ParserUtil.addIpv4Address(parser);

		parseParameters(parser, args);

		printParameters(KEY_OPERATION, new String[] {KEY_ICS_BACKHAUL_INTERFACE, KEY_IP});

		String bhi = resource.getString(KEY_ICS_BACKHAUL_INTERFACE);
		String ip = resource.getString(KEY_IP);

		// prepare identifiers
		Identity bhiIdentifier = IcsIdentifiers.createBackhaulInterface(bhi);
		IpAddress ipIdentifier = Identifiers.createIp4(ip);

		// prepare metadata
		Document metadata = icsmf.createBhiAdd();

		PublishRequest request;
		// update or delete
		if (isUpdate(KEY_OPERATION)) {
			PublishUpdate publishUpdate = Requests.createPublishUpdate(ipIdentifier, bhiIdentifier,
					metadata, MetadataLifetime.forever);
			request = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "ics-meta:bhi-address";
			PublishDelete publishDelete = Requests.createPublishDelete(ipIdentifier, bhiIdentifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.ICS_METADATA_PREFIX,
					IfmapStrings.ICS_METADATA_NS_URI);
			request = Requests.createPublishReq(publishDelete);
		}

		// publish
		publishIfmapData(request);

	}

}
