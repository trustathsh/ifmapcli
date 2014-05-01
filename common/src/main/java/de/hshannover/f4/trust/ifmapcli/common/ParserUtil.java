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
 * This file is part of ifmapcli (common), version 0.1.0, implemented by the Trust@HsH
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
package de.hshannover.f4.trust.ifmapcli.common;

import java.util.Date;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import de.hshannover.f4.trust.ifmapcli.common.enums.EnforcementAction;
import de.hshannover.f4.trust.ifmapcli.common.enums.EventType;
import de.hshannover.f4.trust.ifmapcli.common.enums.FeatureType;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapcli.common.enums.Significance;
import de.hshannover.f4.trust.ifmapcli.common.enums.WlanSecurityEnum;

public class ParserUtil {

	public static final String USER = "user";
	public static final String URL = "url";
	public static final String PASS = "pass";
	public static final String KEYSTORE_PATH = "keystore-path";
	public static final String KEYSTORE_PASS = "keystore-pass";

	public static final String VERBOSE = "verbose";
	
	public static void addConnectionArgumentsTo(ArgumentParser parser) {
		// get environment variables
		String url = System.getenv("IFMAP_URL");
		String user = System.getenv("IFMAP_USER");
		String pass = System.getenv("IFMAP_PASS");
		String keystorePath = System.getenv("IFMAP_TRUSTSTORE_PATH");
		String keystorePass = System.getenv("IFMAP_TRUSTSTORE_PASS");

		// set them if they were defined
		if (url == null)
			url = DefaultConfig.DEFAULT_URL;
		if (user == null)
			user = DefaultConfig.DEFAULT_USER;
		if (pass == null)
			pass = DefaultConfig.DEFAULT_PASS;
		if (keystorePath == null)
			keystorePath = DefaultConfig.DEFAULT_KEYSTORE_PATH;
		if (keystorePass == null)
			keystorePass = DefaultConfig.DEFAULT_KEYSTORE_PASS;

		ArgumentGroup group = parser.addArgumentGroup("IF-MAP parameters");
		group.addArgument("--url").type(String.class).dest(URL).setDefault(url)
				.help("the MAP server URL");
		group.addArgument("--user").type(String.class).dest(USER)
				.setDefault(user).help("IF-MAP basic auth user");
		group.addArgument("--pass").type(String.class).dest(PASS)
				.setDefault(pass).help("user password");
		group.addArgument("--keystore-path").type(String.class)
				.dest(KEYSTORE_PATH).setDefault(keystorePath)
				.help("the keystore file");
		group.addArgument("--keystore-pass").type(String.class)
				.dest(KEYSTORE_PASS).setDefault(keystorePass)
				.help("password for the keystore");
	}

	public static void addCommonArgumentsTo(ArgumentParser parser) {
		parser.addArgument("-v").type(Boolean.class)
				.action(Arguments.storeTrue()).dest(VERBOSE).setDefault(false)
				.help("print logging information");
	}

	public static void addPublishOperation(ArgumentParser parser) {
		parser.addArgument("publish-operation").type(String.class)
				.dest(AbstractClient.KEY_OPERATION).choices("update", "delete")
				.help("the publish operation");
	}

	public static void addPublishOperationWithNotify(ArgumentParser parser) {
		parser.addArgument("publish-operation").type(String.class)
				.dest(AbstractClient.KEY_OPERATION)
				.choices("update", "delete", "notify")
				.help("the publish operation");
	}

	public static void addAccessRequest(ArgumentParser parser) {
		parser.addArgument("access-request").type(String.class)
				.dest(AbstractClient.KEY_ACCESS_REQUEST)
				.help("name of the access-request identifier");
	}

	public static void addDevice(ArgumentParser parser) {
		parser.addArgument("device").type(String.class)
				.dest(AbstractClient.KEY_DEVICE)
				.help("name of the device identifier");
	}

	public static void addIpv4Address(ArgumentParser parser) {
		parser.addArgument("ip-address").type(String.class)
				.dest(AbstractClient.KEY_IP)
				.help("value of the ip-address identifier");
	}

	public static void addMacAddress(ArgumentParser parser) {
		parser.addArgument("mac-address").type(String.class)
				.dest(AbstractClient.KEY_MAC)
				.help("value of the mac identifier");
	}

	public static void addUsernameIdentity(ArgumentParser parser) {
		parser.addArgument("username").type(String.class)
				.dest(AbstractClient.KEY_IDENTITY_USERNAME)
				.help("username value of the identity identifier");
	}

	public static void addCapability(ArgumentParser parser) {
		parser.addArgument("capability").type(String.class)
				.dest(AbstractClient.KEY_CAP_NAME)
				.help("name of the capability metadatum");
	}

	public static void addAdministrativeDomain(ArgumentParser parser) {
		parser.addArgument("--administrative-domain").type(String.class)
				.dest(AbstractClient.KEY_ADMINISTRATIVE_DOMAIN)
				.help("value of the administrative domain");
	}

	public static void addDeviceAttribute(ArgumentParser parser) {
		parser.addArgument("device-attribute").type(String.class)
				.dest(AbstractClient.KEY_ATTR)
				.help("value of the device-attribute metadatum");
	}

	public static void addIdentifierType(ArgumentParser parser, IdType... types) {
		parser.addArgument("identifier-type").type(IdType.class)
				.dest(AbstractClient.KEY_IDENTIFIER_TYPE).choices(types)
				.help("the type of the identifier");
	}

	public static void addIdentifier(ArgumentParser parser) {
		parser.addArgument("identifier").type(String.class)
				.dest(AbstractClient.KEY_IDENTIFIER).help("the identifier");
	}
	
	public static void addExIdentifier(ArgumentParser parser) {
		parser.addArgument("extended-identifier").type(Arguments.fileType().verifyCanRead())
				.dest(AbstractClient.KEY_EX_IDENTIFIER).help("the path to the xml file");
	}
	
	public static void addElementName(ArgumentParser parser) {
		parser.addArgument("--element-name").type(String.class).setDefault("")
				.dest(AbstractClient.KEY_ELEMENT_NAME).help("the name of extended metadata");
	}
	
	public static void addFileInSystemIn(ArgumentParser parser) {
		parser.addArgument("--in").type(Arguments.fileType().acceptSystemIn().verifyCanRead())
				.dest(AbstractClient.KEY_FILE_IN_SYSTEM_IN).help("filename or - for system in");
	}
	
	public static void addCardinality(ArgumentParser parser) {
		parser.addArgument("--cardinality").type(String.class)
				.dest(AbstractClient.KEY_CARDINALITY).choices("singleValue","multiValue").setDefault("singleValue")
				.help("the cardinality of extended metadata");
	}
	
	public static void addAttributeName(ArgumentParser parser) {
		parser.addArgument("--attribute-name").type(String.class)
				.dest(AbstractClient.KEY_ATTRIBUTE_NAME).setDefault("")
				.help("the name of the attribute of a extended metadata");
	}
	
	public static void addAttributeValue(ArgumentParser parser) {
		parser.addArgument("--attribute-value").type(String.class)
				.dest(AbstractClient.KEY_ATTRIBUTE_VALUE).setDefault("")
				.help("the value of the attribute of a extended metadata");
	}
	
	public static void addSecIdentifierType(ArgumentParser parser, IdType... types) {
		parser.addArgument("--sec-identifier-type").type(IdType.class)
				.dest(AbstractClient.KEY_SEC_IDENTIFIER_TYPE).choices(types)
				.help("the type of the second identifier");
	}
	
	public static void addSecIdentifier(ArgumentParser parser) {
		parser.addArgument("--sec-identifier").type(String.class)
				.dest(AbstractClient.KEY_SEC_IDENTIFIER)
				.help("the second identifier name or filename for extended identifier");
	}
	
	
	public static void addOtherIdentifierType(ArgumentParser parser,
			IdType... types) {
		parser.addArgument("other-identifier-type").type(IdType.class)
				.dest(AbstractClient.KEY_OTHER_IDENTIFIER_TYPE).choices(types)
				.help("the type of the other identifier");
	}

	public static void addOtherIdentifier(ArgumentParser parser) {
		parser.addArgument("other-identifier").type(String.class)
				.dest(AbstractClient.KEY_OTHER_IDENTIFIER)
				.help("the identifier");
	}

	public static void addRole(ArgumentParser parser) {
		parser.addArgument("role").type(String.class)
				.dest(AbstractClient.KEY_ROLE)
				.help("value of the role metadatum");
	}

	public static void addMatchLinks(ArgumentParser parser) {
		parser.addArgument("--match-links", "-ml")
				.type(String.class)
				.dest(AbstractClient.KEY_MATCH_LINKS)
				.help("filter for match-links, example: meta:ip-mac. (default is match-all)");
	}

	public static void addMaxDepth(ArgumentParser parser) {
		parser.addArgument("--max-depth", "-md").type(Integer.class)
				.dest(AbstractClient.KEY_MAX_DEPTH).setDefault(0)
				.help("maximum depth for search.");
	}

	public static void addMaxSize(ArgumentParser parser) {
		parser.addArgument("--max-size", "-ms")
				.type(Integer.class)
				.dest(AbstractClient.KEY_MAX_SIZE)
				.help("maximum size for search results. (default is based on MAPS)");
	}

	public static void addResultFilter(ArgumentParser parser) {
		parser.addArgument("--result-filter", "-rf")
				.type(String.class)
				.dest(AbstractClient.KEY_RESULT_FILTER)
				.help("result-filter for search results, example: meta:ip-mac (default is match-all).");
	}

	public static void addTerminalIdentifierType(ArgumentParser parser) {
		parser.addArgument("--terminal-identifier-type", "-tt")
				.type(String.class)
				.dest(AbstractClient.KEY_TERMINAL_IDENTIFIER_TYPE)
				.help("comma-separated type of the terminal identifier(s): ip-address,mac-address,device,access-request,identity");
	}

	public static void addNamespacePrefix(ArgumentParser parser) {
		parser.addArgument("--namespace-prefix", "-np").type(String.class)
				.dest(AbstractClient.KEY_NAMESPACE_PREFIX)
				.help("custom namespace prefix, example: foo");
	}

	public static void addNamespaceUri(ArgumentParser parser) {
		parser.addArgument("--namespace-uri", "-nu")
				.type(String.class)
				.dest(AbstractClient.KEY_NAMESPACE_URI)
				.help("custom namespace URI. example: http://www.foo.bar/2012/ifmap-metadata/1");
	}

	public static void addPublisherId(ArgumentParser parser) {
		parser.addArgument("--publisher-id", "-p").type(String.class)
				.dest(AbstractClient.KEY_PUBLISHER_ID).help("the publisher id");
	}

	public static void addPerf1NumberRequests(ArgumentParser parser) {
		parser.addArgument("requests").type(Integer.class)
				.dest(AbstractClient.KEY_NUMBER_REQUESTS)
				.help("number of publish requests");
	}

	public static void addPerf1NumberUpdates(ArgumentParser parser) {
		parser.addArgument("updates").type(Integer.class)
				.dest(AbstractClient.KEY_NUMBER_UPDATES)
				.help("number of update elements per request");
	}

	public static void addPerf1NumberSprints(ArgumentParser parser) {
		parser.addArgument("sprint-size").type(Integer.class)
				.dest(AbstractClient.KEY_NUMBER_SPRINTS)
				.help("size of one sprint");
	}

	public static void addQualifier(ArgumentParser parser) {
		parser.addArgument("--qualifier").type(String.class)
				.dest(AbstractClient.KEY_QUALIFIER)
				.help("the qualifier for the request-for-investigation");
	}

	public static void addVlanNumber(ArgumentParser parser) {
		parser.addArgument("--vlan-number").type(Integer.class)
				.dest(AbstractClient.KEY_VLAN_NUMBER).help("vlan number");
	}

	public static void addVlanName(ArgumentParser parser) {
		parser.addArgument("--vlan-name").type(String.class)
				.dest(AbstractClient.KEY_VLAN_NAME).help("vlan name");
	}

	public static void addPort(ArgumentParser parser) {
		parser.addArgument("--port").type(Integer.class)
				.dest(AbstractClient.KEY_PORT).help("port");
	}

	public static void addManufacturer(ArgumentParser parser) {
		// characteristics (min=0, max=1)
		parser.addArgument("--manufacturer").type(String.class)
				.dest(AbstractClient.KEY_MANUFACTURER)
				.help("manufacturer of the device");
	}

	public static void addModel(ArgumentParser parser) {
		// characteristics (min=0, max=1)
		parser.addArgument("--model").type(String.class)
				.dest(AbstractClient.KEY_MODEL)
				.help("model type of the device");
	}

	public static void addOs(ArgumentParser parser) {
		// characteristics (min=0, max=1)
		parser.addArgument("--os").type(String.class)
				.dest(AbstractClient.KEY_OS).help("OS of the device");
	}

	public static void addOsVersion(ArgumentParser parser) {
		// characteristics (min=0, max=1)
		parser.addArgument("--os-version").type(String.class)
				.dest(AbstractClient.KEY_OS_VERSION)
				.help("OS version of the device");
	}

	public static void addDeviceType(ArgumentParser parser) {
		// characteristics (min=0, max=unbounded)
		// TODO allow more than one argument
		parser.addArgument("--device-type").type(String.class)
				.dest(AbstractClient.KEY_DEVICE_TYPE).help("device type");
	}

	public static void addDiscoveredTime(ArgumentParser parser) {
		// characteristics (min=1, max=1)
		parser.addArgument("discovered-time").type(String.class)
				.dest(AbstractClient.KEY_DISCOVERED_TIME)
				.setDefault(Common.getTimeAsXsdDateTime(new Date()))
				.help("time of discovery");
	}

	public static void addDiscovererId(ArgumentParser parser) {
		// characteristics (min=1, max=1)
		parser.addArgument("discoverer-id").type(String.class)
				.dest(AbstractClient.KEY_DISCOVERER_ID)
				.setDefault("ifmapj")
				.help("ID of discoverer");
	}

	public static void addDiscoveryMethod(ArgumentParser parser) {
		// characteristics (min=1, max=unbounded)
		// TODO allow more than one argument
		parser.addArgument("discovery-method").type(String.class)
				.dest(AbstractClient.KEY_DISCOVERY_METHOD)
				.help("method of discovery");
	}

	public static void addEventName(ArgumentParser parser) {
		parser.addArgument("name").type(String.class)
				.dest(AbstractClient.KEY_EVENT_NAME)
				.help("the name of the event");
	}

	public static void addMagnitude(ArgumentParser parser) {
		parser.addArgument("--magnitude").type(Integer.class)
				.dest(AbstractClient.KEY_MAGNITUDE).setDefault(0)
				.choices(Arguments.range(0, 100))
				.help("the magnitude");
	}

	public static void addConfidence(ArgumentParser parser) {
		parser.addArgument("--confidence").type(Integer.class)
				.dest(AbstractClient.KEY_CONFIDENCE).setDefault(0)
				.choices(Arguments.range(0, 100))
				.help("the confidence");
	}

	public static void addSignificance(ArgumentParser parser) {
		parser.addArgument("--significance")
				.type(Significance.class)
				.setDefault(Significance.informational)
				.choices(Significance.critical, Significance.important,
						Significance.informational)
				.dest(AbstractClient.KEY_SIGNIFICANCE)
				.help("the significance");
	}

	public static void addEventType(ArgumentParser parser) {
		parser.addArgument("--type")
				.type(EventType.class)
				.choices(EventType.p2p, EventType.cve,
						EventType.botnet_infection, EventType.worm_infection,
						EventType.excessive_flows, EventType.behavioral_change,
						EventType.policy_violation, EventType.other)
				.dest(AbstractClient.KEY_EVENT_TYPE)
				.setDefault(EventType.other).help("the type of the event");
	}

	public static void addInformation(ArgumentParser parser) {
		parser.addArgument("--information").type(String.class)
				.dest(AbstractClient.KEY_INFORMATION)
				.help("\"human consumable\" informational string");
	}

	public static void addEventVulnerabilityUri(ArgumentParser parser) {
		parser.addArgument("--vulnerability-uri").type(String.class)
				.dest(AbstractClient.KEY_VULNERABILITY_URI)
				.help("URI of the CVE if type cve is used");
	}

	public static void addOtherTypeDefinition(ArgumentParser parser) {
		parser.addArgument("--other-type-def").type(String.class)
				.dest(AbstractClient.KEY_OTHERTYPE_DEFINITION)
				.help("other-type-definition");
	}

	public static void addFeatureTargetDevice(ArgumentParser parser) {
		parser.addArgument("target-device").type(String.class)
				.dest(AbstractClient.KEY_TARGET_DEVICE)
				.help("the target device identifier");
	}

	public static void addFeatureTreeDepth(ArgumentParser parser) {
		parser.addArgument("tree-depth").type(Integer.class)
				.dest(AbstractClient.KEY_DEPTH)
				.help("depth of the feature tree");
	}

	public static void addFeatureMaxChilds(ArgumentParser parser) {
		parser.addArgument("max-childs").type(Integer.class)
				.dest(AbstractClient.KEY_MAX_CHILDS)
				.help("max childs per category");
	}

	public static void addFeatureMaxFeatures(ArgumentParser parser) {
		parser.addArgument("max-features").type(Integer.class)
				.dest(AbstractClient.KEY_MAX_FEATURES)
				.help("max features per category");
	}

	public static void addFeaturePurge(ArgumentParser parser) {
		parser.addArgument("--purge", "-p").type(Boolean.class)
				.action(Arguments.storeTrue()).dest(AbstractClient.KEY_PURGE)
				.setDefault(false).help("purge subcategory-of");
	}

	public static void addFeatureId(ArgumentParser parser) {
		parser.addArgument("feature-id").type(String.class)
				.dest(AbstractClient.KEY_FEATURE_ID)
				.help("full-qualified instance-aware feature id");
	}

	public static void addFeatureType(ArgumentParser parser) {
		parser.addArgument("feature-type")
				.type(FeatureType.class)
				.dest(AbstractClient.KEY_FEATURE_TYPE)
				.choices(FeatureType.arbitrary, FeatureType.qualified,
						FeatureType.quantitive).help("feature type");
	}

	public static void addFeatureValue(ArgumentParser parser) {
		parser.addArgument("feature-value").type(String.class)
				.dest(AbstractClient.KEY_FEATURE_VALUE).help("feature value");
	}

	public static void addFeatureContextTimestamp(ArgumentParser parser) {
		parser.addArgument("--ctxp-timestamp", "-ctxt").type(String.class)
				.dest(AbstractClient.KEY_CTX_TIMESTAMP)
				.setDefault(Common.getTimeAsXsdDateTime(new Date()))
				.help("context: timestamp");
	}

	public static void addFeatureContextPosition(ArgumentParser parser) {
		parser.addArgument("--ctxp-position", "-ctxp").type(String.class)
				.dest(AbstractClient.KEY_CTX_POSITION).setDefault("work")
				.help("context: position");
	}

	public static void addFeatureContextOtherDevices(ArgumentParser parser) {
		parser.addArgument("--ctxp-other-devices", "-ctxo").type(String.class)
				.dest(AbstractClient.KEY_CTX_OTHER_DEVICES).setDefault("none")
				.help("context: other devices");
	}

	public static void addDhcpServer(ArgumentParser parser) {
		parser.addArgument("--dhcp-server", "-ds").type(String.class)
		.dest(AbstractClient.KEY_DHCP_SERVER).setDefault("ip-mac-cli")
		.help("name of the DHCP server");
	}

	public static void addLocationInfoTypes(ArgumentParser parser) {
		parser.addArgument("--location-info-type", "-lt").type(String.class)
		.dest(AbstractClient.KEY_LOCATION_INFORMATION_TYPE).nargs("+").required(true)
		.help("type(s) of the location info");
	}
	
	public static void addLocationInfoValues(ArgumentParser parser) {
		parser.addArgument("--location-info-value", "-lv").type(String.class)
		.dest(AbstractClient.KEY_LOCATION_INFORMATION_VALUE).nargs("+").required(true)
		.help("values of the location info");
	}

	public static void addEnforcementAction(ArgumentParser parser) {
		parser.addArgument("enforcement-action").type(EnforcementAction.class)
		.choices(EnforcementAction.block, EnforcementAction.other, EnforcementAction.quarantine)
		.dest(AbstractClient.KEY_ENFORCEMENT_ACTION)
		.help("type of the enforcement action");
	}

	public static void addEnforcementReason(ArgumentParser parser) {
		parser.addArgument("--enforcement-reason", "-er").type(String.class)
		.dest(AbstractClient.KEY_ENFORCEMENT_REASON)
		.help("reason of the enforcement");
	}
	
	public static void addUnexpBehaviorType(ArgumentParser parser) {
		parser.addArgument("--unexp-behavior-type", "-ubt").type(String.class)
		.dest(AbstractClient.KEY_UNEXP_BEHAVIOR_TYPE)
		.help("type of the unexpected behavior");
	}

	public static void addWlanInfoSsid(ArgumentParser parser) {
		parser.addArgument("--wlan-info-ssid", "-ws").type(String.class)
		.dest(AbstractClient.KEY_WLAN_INFORMATION_SSID)
		.help("SSID of the WLAN");
	}

	public static void addWlanInfoGroupSecurity(ArgumentParser parser) {
		parser.addArgument("wlan-info-group-security").type(WlanSecurityEnum.class)
		.dest(AbstractClient.KEY_WLAN_INFORMATION_GROUP_SECURITY).required(true)
		.choices(WlanSecurityEnum.bip,
				WlanSecurityEnum.ccmp,
				WlanSecurityEnum.open,
				WlanSecurityEnum.other,
				WlanSecurityEnum.tkip,
				WlanSecurityEnum.wep)
		.help("type of the WLAN group security");
	}

	public static void addWlanInfoUnicastSecurity(ArgumentParser parser) {
		parser.addArgument("--wlan-info-unicast-security", "-wus").type(WlanSecurityEnum.class)
		.dest(AbstractClient.KEY_WLAN_INFORMATION_UNICAST_SECURITY).nargs("+").required(true)
		.choices(WlanSecurityEnum.bip,
				WlanSecurityEnum.ccmp,
				WlanSecurityEnum.open,
				WlanSecurityEnum.other,
				WlanSecurityEnum.tkip,
				WlanSecurityEnum.wep)
		.help("type(s) of the WLAN unicast security");
	}

	public static void addWlanInfoManagementSecurity(ArgumentParser parser) {
		parser.addArgument("--wlan-info-management-security", "-wms").type(WlanSecurityEnum.class)
		.dest(AbstractClient.KEY_WLAN_INFORMATION_MANAGEMENT_SECURITY).nargs("+").required(true)
		.choices(WlanSecurityEnum.bip,
				WlanSecurityEnum.ccmp,
				WlanSecurityEnum.open,
				WlanSecurityEnum.other,
				WlanSecurityEnum.tkip,
				WlanSecurityEnum.wep)
		.help("type(s) of the WLAN management security");
	}
}
