package de.fhhannover.inform.trust.ifmapcli;

/*
 * #%L
 * ====================================================
 *   _____                _     ____  _____ _   _ _   _
 *  |_   _|_ __ _   _ ___| |_  / __ \|  ___| | | | | | |
 *    | | | '__| | | / __| __|/ / _` | |_  | |_| | |_| |
 *    | | | |  | |_| \__ \ |_| | (_| |  _| |  _  |  _  |
 *    |_| |_|   \__,_|___/\__|\ \__,_|_|   |_| |_|_| |_|
 *                             \____/
 * 
 * =====================================================
 * 
 * Fachhochschule Hannover 
 * (University of Applied Sciences and Arts, Hannover)
 * Faculty IV, Dept. of Computer Science
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 * 
 * Email: trust@f4-i.fh-hannover.de
 * Website: http://trust.inform.fh-hannover.de/
 * 
 * This file is part of Ifmapcli, version 0.0.2, implemented by the Trust@FHH 
 * research group at the Fachhochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2013 Trust@FHH
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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.TrustManager;

import org.w3c.dom.Document;

import de.fhhannover.inform.trust.ifmapcli.common.Common;
import de.fhhannover.inform.trust.ifmapcli.common.Config;
import de.fhhannover.inform.trust.ifmapj.IfmapJ;
import de.fhhannover.inform.trust.ifmapj.IfmapJHelper;
import de.fhhannover.inform.trust.ifmapj.binding.IfmapStrings;
import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.exception.InitializationException;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IdentityType;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishDelete;
import de.fhhannover.inform.trust.ifmapj.messages.PublishElement;
import de.fhhannover.inform.trust.ifmapj.messages.PublishRequest;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ifmapj.metadata.StandardIfmapMetadataFactory;
import de.fhhannover.inform.trust.ifmapj.metadata.WlanSecurityEnum;
import de.fhhannover.inform.trust.ifmapj.metadata.WlanSecurityType;

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
public class Pdp {
	public static final String CMD = "pdp";
	public static final int MIN_ARGS = 4;		// update|delete, username, ip, mac
	public static final int EXPECTED_ARGS = 9;	// update|delete, username, ip, mac
												// url, user, pass,
												// keystorePath, keystorePass
	
	private StandardIfmapMetadataFactory mStandardMetaFactory;
	
	PublishRequest mPublishRequest;
	
	private boolean mIsUpdate;
	private String mUsername;
	private String mIp;
	private String mMac;
	
	private Config mConfig;
	
	private SSRC mSsrc;
	
	public Pdp(String operation, String username, String ip, String mac,
			Config config) {
		// init factories
		mStandardMetaFactory = IfmapJ.createStandardMetadataFactory();
		
		// save parameters
		mIsUpdate = Common.isUpdate(operation);
		mUsername = username;
		mIp = ip;
		mMac = mac;
		
		// save configuration
		mConfig = config;
		
		// build ifmapj request object
		mPublishRequest = Requests.createPublishReq();
	}
	
	private void initSsrc() throws FileNotFoundException, InitializationException {
		InputStream is = Common.prepareTruststoreIs(mConfig.getTruststorePath());
		TrustManager[] tms = IfmapJHelper.getTrustManagers(is, mConfig.getTruststorePass());
		mSsrc = IfmapJ.createSSRC(mConfig.getUrl(), mConfig.getUser(), mConfig.getPass(), tms);
	}

	public void publish() throws FileNotFoundException, IfmapErrorResult, IfmapException{
		Identifier accessRequest;
		Identifier macAddress, ipAddress;
		Identifier identity;
		Identifier pepDevice, pdpDevice, endpointDevice;
		
		// establish session
		initSsrc();
		mSsrc.newSession();
		
		// create identifiers
		accessRequest = Identifiers.createAr(mSsrc.getPublisherId() + ":" + mUsername.hashCode());
		macAddress = Identifiers.createMac(mMac);
		ipAddress = Identifiers.createIp4(mIp);
		identity = Identifiers.createIdentity(IdentityType.userName, mUsername);
		pepDevice = Identifiers.createDev("example-pep-id:" + mUsername.hashCode());
		pdpDevice = Identifiers.createDev("example-pdp-id:" + mUsername.hashCode());
		endpointDevice = Identifiers.createDev("example-endpoint-id:" + mUsername.hashCode());
		
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
		mSsrc.publish(mPublishRequest);
		
		// clean up
		mSsrc.endSession();
		mSsrc.closeTcpConnection();
	}

	/**
	 * wlan-information metadata on the link between the access-request
	 * identifier and the device identifier of the PEP
	 * 
	 * @param accessRequest
	 * @param pepDevice
	 */
	private void addWlanInfo(Identifier accessRequest, Identifier pepDevice) {
		// create and set wlan-information metadata
		WlanSecurityType wlan1 = new WlanSecurityType(WlanSecurityEnum.ccmp, null);
		WlanSecurityType wlan2 = new WlanSecurityType(WlanSecurityEnum.other, "my own wlan security type");
		WlanSecurityType wlan3 = new WlanSecurityType(WlanSecurityEnum.tkip, null);
		List<WlanSecurityType> unicastSec = new ArrayList<WlanSecurityType>();
		unicastSec.add(wlan1);
		List<WlanSecurityType> managementSec = new ArrayList<WlanSecurityType>();
		managementSec.add(wlan3);
		Document metadata = mStandardMetaFactory.createWlanInformation("eduroam", unicastSec, wlan2, managementSec);
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, pepDevice, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, pepDevice, "meta:wlan-information");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);			
	}

	/**
	 * layer2-information metadata on the link between the access-request
	 * identifier and the device identifier of the PEP (when authenticated at
	 * layer 2 or otherwise available)
	 * 
	 * @param accessRequest
	 * @param pepDevice
	 */
	private void addLayer2(Identifier accessRequest, Identifier pepDevice) {
		Document metadata = mStandardMetaFactory.createLayer2Information(96, "vlan1", 1, null);
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, pepDevice, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, pepDevice, "meta:layer2-information");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);		
	}

	/**
	 * role metadata on the link between the access-request identifier and an
	 * identity identifier
	 * 
	 * @param accessRequest
	 * @param identity
	 */
	private void addRole(Identifier accessRequest, Identifier identity) {
		Document metadata = mStandardMetaFactory.createRole("employee");
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, identity, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, identity, "meta:role");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);
	}

	/**
	 * device-characteristic metadata on the link between the access-request
	 * identifier and its own device identifier
	 * 
	 * @param accessRequest
	 * @param pdpDevice
	 */
	private void addDevChar(Identifier accessRequest, Identifier pdpDevice) {
		Calendar cal = Calendar.getInstance();
		Document metadata = mStandardMetaFactory.createDevChar("manufacturer1", "model1", "Linux", "3.0.0", "pdp", Common.getTimeAsXsdDateTime(cal.getTime()), "TNC Server", "TPM Assessment");
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, pdpDevice, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, pdpDevice, "meta:device-characteristic");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);
	}

	/**
	 * device-attribute metadata on the link between the access-request
	 * identifier and the endpoint's device identifier
	 * 
	 * @param accessRequest
	 * @param endpointDevice
	 */
	private void addDevAttr(Identifier accessRequest, Identifier endpointDevice) {
		Document metadata = mStandardMetaFactory.createDevAttr("looks pretty");
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, endpointDevice, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, endpointDevice, "meta:device-attribute");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);
	}

	/**
	 * capability metadata on the access-request identifier
	 * 
	 * @param accessRequest
	 */
	private void addCap(Identifier accessRequest) {
		Document metadata = mStandardMetaFactory.createCapability("trustworthy, for sure!");
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, "meta:capability");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);	
	}

	/**
	 * authenticated-as metadata on the link between the access-request
	 * identifier and any identity identifiers associated with the user's
	 * authenticated identity
	 * 
	 * @param accessRequest
	 * @param identity
	 */
	private void addAuthAs(Identifier accessRequest, Identifier identity) {
		Document metadata = mStandardMetaFactory.createAuthAs();
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, identity, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, identity, "meta:authenticated-as");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);			
	}

	/**
	 * authenticated-by metadata on the link between the access-request
	 * identifier and the PDP's device identifier
	 * 
	 * @param accessRequest
	 * @param pdpDevice
	 */
	private void addAuthBy(Identifier accessRequest, Identifier pdpDevice) {
		Document metadata = mStandardMetaFactory.createAuthBy();
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, pdpDevice, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, pdpDevice, "meta:authenticated-by");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);		
	}

	/**
	 * access-request-ip metadata on the link between the access-request
	 * identifier and the endpoint's ip-address identifier (when authenticated
	 * at layer 3 or otherwise available)
	 * 
	 * @param accessRequest
	 * @param ipAddress
	 */
	private void addArIp(Identifier accessRequest, Identifier ipAddress) {
		Document metadata = mStandardMetaFactory.createArIp();
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, ipAddress, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, ipAddress, "meta:access-request-ip");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);
	}

	/**
	 * access-request-mac metadata on the link between the access-request
	 * identifier and the endpoint's mac-address identifier (when authenticated
	 * at layer 2 or otherwise available)
	 * 
	 * @param accessRequest
	 * @param macAddress
	 */
	private void addArMac(Identifier accessRequest, Identifier macAddress) {
		Document metadata = mStandardMetaFactory.createArMac();
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, macAddress, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, macAddress, "meta:access-request-mac");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);
	}

	/**
	 * access-request-device metadata on the link between the access-request
	 * identifier and the endpoint's device identifier
	 * @param accessRequest
	 * @param endpointDevice
	 */
	private void addArDevice(Identifier accessRequest, Identifier endpointDevice) {
		Document metadata = mStandardMetaFactory.createArDev();
		PublishElement publishEl;
		if(mIsUpdate){
			publishEl = Requests.createPublishUpdate(accessRequest, endpointDevice, metadata, MetadataLifetime.forever);					
		} else {
			PublishDelete publishDelete = Requests.createPublishDelete(accessRequest, endpointDevice, "meta:access-request-device");
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,	IfmapStrings.STD_METADATA_NS_URI);
			publishEl = publishDelete;
		}
		mPublishRequest.addPublishElement(publishEl);
	}

	/**
	 * @param args - this tool only expects 4 mandatory parameters<br/>
	 * 				<ul>
	 * 					<li>arg[0]: update|delete</li>
	 * 					<li>arg[1]: username</li>
	 * 					<li>arg[2]: ip</li>
	 * 					<li>arg[3]: mac</li>
	 * 				</ul>
	 */
	public static void main(String[] args) {
		String op, username, ip, mac;
		Config cfg;		
		// check number of mandatory command line arguments
		if(args.length < MIN_ARGS){
			Pdp.usage();
			return;
		}
		
		// parse mandatory command line arguments
		op = args[0];
		username = args[1];
		ip = args[2];
		mac = args[3];
		if(Common.isUpdateorDelete(op) == false){
			Pdp.usage();
			return;
		}
		
		// check and load optional parameters
		cfg = Common.checkAndLoadParams(args, EXPECTED_ARGS);
		System.out.println(CMD + " uses config " + cfg);
		
		// publish
		try {
			// create pdp
			Pdp pdp = new Pdp(op, username, ip, mac, cfg);
			pdp.publish();
		} catch (InitializationException e) {
			System.out.println(e.getDescription() + " " + e.getMessage());
		} catch (IfmapErrorResult e) {
			System.out.println(e.getErrorString());
		} catch (IfmapException e) {
			System.out.println(e.getDescription() + " " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private static void usage() {
		System.out.println("usage:\n" +
				"\t" + Pdp.CMD + " update|delete username ip mac " +
				"[url user pass truststore truststorePass]");
		System.out.println(Common.USAGE);
	}
}
