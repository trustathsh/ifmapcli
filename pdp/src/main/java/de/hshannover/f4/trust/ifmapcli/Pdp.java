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
 * This file is part of ifmapcli (pdp), version 0.3.2, implemented by the Trust@HsH
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.identifier.IdentityType;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishElement;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum;
import de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityType;

/**
 * A simple tool that acts like a TNC PDP with IF-MAP support.<br/>
 * The following PDP mandatory metadata objects are published:<br/>
 * <ul>
 * 	<li>access-request-device</li>
 * 	<li>access-request-mac</li>
 * 	<li>access-request-ip</li>
 * 	<li>authenticated-by</li>
 * </ul>
 *
 * In addition, the following optional metadata objects are published:<br/>
 * <ul>
 * 	<li>authenticated-as</li>
 * 	<li>capability</li>
 * 	<li>device-attribute</li>
 * 	<li>device-characteristic</li>
 * 	<li>role</li>
 * 	<li>layer2-information</li>
 * 	<li>wlan-information</li>
 * </ul>
 *
 * When metadata is published, the lifetime is set to be 'forever'.<br/>
 * The values of device and access request identifiers are created in such<br/>
 * a way that there is no connection between the subgraphs for each user.
 *
 * @author ib
 *
 */
public class Pdp extends AbstractClient {

	private static PublishRequest publishRequest;

	private static boolean isUpdate;
	private static String username;
	private static String ip;
	private static String mac;

	private static void publish() {
		Identifier accessRequest;
		Identifier macAddress, ipAddress;
		Identifier identity;
		Identifier pepDevice, pdpDevice, endpointDevice;

		try {
			// establish session
			SSRC ssrc = createSSRC();
			ssrc.newSession();
			
			// create identifiers
			accessRequest = Identifiers.createAr(ssrc.getPublisherId() + ":" + username.hashCode());
			macAddress = Identifiers.createMac(mac);
			ipAddress = Identifiers.createIp4(ip);
			identity = Identifiers.createIdentity(IdentityType.userName, username);
			pepDevice = Identifiers.createDev("example-pep-id:" + username.hashCode());
			pdpDevice = Identifiers.createDev("example-pdp-id:" + username.hashCode());
			endpointDevice = Identifiers.createDev("example-endpoint-id:" + username.hashCode());
			
			// add mandatory metadata to publish request
			addArDevice(accessRequest, endpointDevice);
			addArMac(accessRequest, macAddress);
			addArIp(accessRequest, ipAddress);
			addAuthBy(accessRequest, pdpDevice);
			
			// add optional metadata to publish request
			addAuthAs(accessRequest, identity);
			addCap(accessRequest);
			addDevAttr(accessRequest, endpointDevice);
			addDevChar(accessRequest, pdpDevice);
			addRole(accessRequest, identity);
			addLayer2(accessRequest, pepDevice);
			addWlanInfo(accessRequest, pepDevice);
			
			// do publish
			ssrc.publish(publishRequest);
			
			// clean up
			ssrc.endSession();
			ssrc.closeTcpConnection();		
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * wlan-information metadata on the link between the access-request
	 * identifier and the device identifier of the PEP
	 *
	 * @param accessRequest
	 * @param pepDevice
	 */
	private static void addWlanInfo(Identifier accessRequest, Identifier pepDevice) {
		// create and set wlan-information metadata
		WlanSecurityType wlan1 = new WlanSecurityType(WlanSecurityEnum.ccmp, null);
		WlanSecurityType wlan2 = new WlanSecurityType(WlanSecurityEnum.other, "my own wlan security type");
		WlanSecurityType wlan3 = new WlanSecurityType(WlanSecurityEnum.tkip, null);
		List<WlanSecurityType> unicastSec = new ArrayList<WlanSecurityType>();
		unicastSec.add(wlan1);
		List<WlanSecurityType> managementSec = new ArrayList<WlanSecurityType>();
		managementSec.add(wlan3);
		Document metadata = mf.createWlanInformation("eduroam", unicastSec, wlan2, managementSec);
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, pepDevice, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, pepDevice, "meta:wlan-information");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * layer2-information metadata on the link between the access-request
	 * identifier and the device identifier of the PEP (when authenticated at
	 * layer 2 or otherwise available)
	 *
	 * @param accessRequest
	 * @param pepDevice
	 */
	private static void addLayer2(Identifier accessRequest, Identifier pepDevice) {
		Document metadata = mf.createLayer2Information(96, "vlan1", 1, null);
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, pepDevice, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, pepDevice, "meta:layer2-information");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * role metadata on the link between the access-request identifier and an
	 * identity identifier
	 *
	 * @param accessRequest
	 * @param identity
	 */
	private static void addRole(Identifier accessRequest, Identifier identity) {
		Document metadata = mf.createRole("employee");
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, identity, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, identity, "meta:role");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * device-characteristic metadata on the link between the access-request
	 * identifier and its own device identifier
	 *
	 * @param accessRequest
	 * @param pdpDevice
	 */
	private static void addDevChar(Identifier accessRequest, Identifier pdpDevice) {
		Calendar cal = Calendar.getInstance();
		Document metadata = mf.createDevChar("manufacturer1", "model1", "Linux", "3.0.0", "pdp", Common.getTimeAsXsdDateTime(cal.getTime()), "TNC Server", "TPM Assessment");
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, pdpDevice, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, pdpDevice, "meta:device-characteristic");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * device-attribute metadata on the link between the access-request
	 * identifier and the endpoint's device identifier
	 *
	 * @param accessRequest
	 * @param endpointDevice
	 */
	private static void addDevAttr(Identifier accessRequest, Identifier endpointDevice) {
		Document metadata = mf.createDevAttr("looks pretty");
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, endpointDevice, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, endpointDevice, "meta:device-attribute");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * capability metadata on the access-request identifier
	 *
	 * @param accessRequest
	 */
	private static void addCap(Identifier accessRequest) {
		Document metadata = mf.createCapability("trustworthy, for sure!");
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, "meta:capability");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * authenticated-as metadata on the link between the access-request
	 * identifier and any identity identifiers associated with the user's
	 * authenticated identity
	 *
	 * @param accessRequest
	 * @param identity
	 */
	private static void addAuthAs(Identifier accessRequest, Identifier identity) {
		Document metadata = mf.createAuthAs();
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, identity, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, identity, "meta:authenticated-as");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * authenticated-by metadata on the link between the access-request
	 * identifier and the PDP's device identifier
	 *
	 * @param accessRequest
	 * @param pdpDevice
	 */
	private static void addAuthBy(Identifier accessRequest, Identifier pdpDevice) {
		Document metadata = mf.createAuthBy();
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, pdpDevice, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, pdpDevice, "meta:authenticated-by");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * access-request-ip metadata on the link between the access-request
	 * identifier and the endpoint's ip-address identifier (when authenticated
	 * at layer 3 or otherwise available)
	 *
	 * @param accessRequest
	 * @param ipAddress
	 */
	private static void addArIp(Identifier accessRequest, Identifier ipAddress) {
		Document metadata = mf.createArIp();
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, ipAddress, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, ipAddress, "meta:access-request-ip");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * access-request-mac metadata on the link between the access-request
	 * identifier and the endpoint's mac-address identifier (when authenticated
	 * at layer 2 or otherwise available)
	 *
	 * @param accessRequest
	 * @param macAddress
	 */
	private static void addArMac(Identifier accessRequest, Identifier macAddress) {
		Document metadata = mf.createArMac();
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, macAddress, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, macAddress, "meta:access-request-mac");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	/**
	 * access-request-device metadata on the link between the access-request
	 * identifier and the endpoint's device identifier
	 * @param accessRequest
	 * @param endpointDevice
	 */
	private static void addArDevice(Identifier accessRequest, Identifier endpointDevice) {
		Document metadata = mf.createArDev();
		PublishElement publishEl;
		if(isUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, endpointDevice, metadata, MetadataLifetime.forever);
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, endpointDevice, "meta:access-request-device");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		publishRequest.addPublishElement(publishEl);
	}

	public static void main(String[] args) {
		command = "pdp";
		
		ArgumentParser parser = createDefaultParser();
		ParserUtil.addPublishOperation(parser);
		ParserUtil.addIpv4Address(parser);
		ParserUtil.addMacAddress(parser);
		ParserUtil.addUsernameIdentity(parser);

		parseParameters(parser, args);

		printParameters(KEY_OPERATION, new String[] {KEY_IP, KEY_MAC, KEY_IDENTITY_USERNAME});
		
		isUpdate = isUpdate(KEY_OPERATION);
		ip = resource.getString(KEY_IP);
		mac = resource.getString(KEY_MAC);
		username = resource.getString(KEY_IDENTITY_USERNAME);
		
		publishRequest = Requests.createPublishReq();
		publish();
	}
}
