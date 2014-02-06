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
 * This file is part of ifmapcli (dev-char), version 0.0.6, implemented by the Trust@HsH
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

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple tool that publishes or deletes device-characteristic metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author bhl
 *
 */
public class DevChar extends AbstractClient {
	
	public static void main(String[] args) {
		command = "dev-char";
		
		final String KEY_OPERATION = "publishOperation";
		final String KEY_IDENTIFIER = "identifier";
		final String KEY_IDENTIFIER_TYPE = "identifierType";
		final String KEY_DEV = "device";
		
		final String KEY_MANUFACTURER = "manufacturer";
		final String KEY_MODEL = "manufacturer";
		final String KEY_OS = "os";
		final String KEY_OS_VERSION = "os-version";
		final String KEY_DEVICE_TYPE = "device-type";
		final String KEY_DISCOVERED_TIME = "discoverer-time";
		final String KEY_DISCOVERER_ID = "discoverer-id";
		final String KEY_DISCOVERY_METHOD = "discovery-method";
		

		ArgumentParser parser = createDefaultParser();
		parser.addArgument("publish-operation")
			.type(String.class)
			.dest(KEY_OPERATION)
			.choices("update", "delete")
			.help("the publish operation");
		parser.addArgument("identifier-type")
			.type(IdType.class)
			.dest(KEY_IDENTIFIER_TYPE)
			.choices(
				IdType.ipv4,
				IdType.ipv6,
				IdType.mac,
				IdType.ar)
			.help("the type of the identifier");
		parser.addArgument("identifier")
			.type(String.class)
			.dest(KEY_IDENTIFIER)
			.help("the identifier");		
		parser.addArgument("device")
			.type(String.class)
			.dest(KEY_DEV)
			.help("name of the device identifier");

		// characteristics (min=0, max=1)
		parser.addArgument("--manufacturer")
			.type(String.class)
			.dest(KEY_MANUFACTURER)
			.help("manufacturer of the device");
		parser.addArgument("--os")
			.type(String.class)
			.dest(KEY_OS)
			.help("OS of the device");
		parser.addArgument("--os-version")
			.type(String.class)
			.dest(KEY_OS_VERSION)
			.help("OS version of the device");

		// characteristics (min=0, max=unbounded)
		parser.addArgument("--device-type")
			.type(String.class)
			.dest(KEY_DEVICE_TYPE)
			.help("device type");
		
		// characteristics (min=1, max=1)
		parser.addArgument("discoverer-time")
			.type(String.class)
			.dest(KEY_DISCOVERED_TIME)
			.help("time of discovery");
		parser.addArgument("discoverer-id")
			.type(String.class)
			.dest(KEY_DISCOVERER_ID)
			.help("ID of discoverer");
		
		// characteristics (min=1, max=unbounded)
		// TODO allow more than one argument
		parser.addArgument("discovery-method")
			.type(String.class)
			.dest(KEY_DISCOVERY_METHOD)
			.help("method of discovery");

		parseParameters(parser, args);
		
		printParameters(KEY_OPERATION, new String[] {KEY_IDENTIFIER_TYPE, KEY_IDENTIFIER, KEY_DEV,
				KEY_MANUFACTURER, KEY_MODEL, KEY_OS, KEY_OS_VERSION, KEY_DEVICE_TYPE,
				KEY_DISCOVERED_TIME, KEY_DISCOVERER_ID, KEY_DISCOVERY_METHOD});

		IdType identifierType = resource.get(KEY_IDENTIFIER_TYPE);
		String identifierName = resource.getString(KEY_IDENTIFIER);
		Identifier identifier = getIdentifier(identifierType, identifierName);
		
		// prepare identifiers
		Identifier devIdentifier = Identifiers.createDev(resource.getString(KEY_DEV));

		// prepare metadata
		String manufacturer = resource.getString(KEY_MANUFACTURER);
		String model = resource.getString(KEY_MODEL);
		String os = resource.getString(KEY_OS);
		String osVersion = resource.getString(KEY_OS_VERSION);
		String deviceType = resource.getString(KEY_DEVICE_TYPE);
		String discovererTime = resource.getString(KEY_DISCOVERED_TIME);
		String discovererId = resource.getString(KEY_DISCOVERER_ID);
		String discoveryMethod = resource.getString(KEY_DISCOVERY_METHOD);
		
		Document metadata = mf.createDevChar(manufacturer, model, os, osVersion, deviceType, discovererTime, discovererId, discoveryMethod);

		PublishRequest request;
		// update or delete
		if (isUpdate(KEY_OPERATION)) {
			PublishUpdate publishUpdate = Requests.createPublishUpdate(identifier, devIdentifier,
					metadata, MetadataLifetime.forever);
			request = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "meta:device-characteristic";
//			String filter = String.format(
//					"meta:device-characteristic[discovered-time='%s' and discoverer-id='%s' and discovery-method='%s']",
//					discovererTime, discovererId, discoveryMethod);
			PublishDelete publishDelete = Requests.createPublishDelete(identifier, devIdentifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			request = Requests.createPublishReq(publishDelete);
		}

		// publish
		publishIfmapData(request);
	}
}
