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
 * This file is part of ifmapcli (common), version 0.3.1, implemented by the Trust@HsH
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
package de.hshannover.f4.trust.ifmapcli.common;

import java.util.ArrayList;
import java.util.List;

import de.hshannover.f4.trust.ifmapcli.common.enums.EnforcementAction;
import de.hshannover.f4.trust.ifmapcli.common.enums.EventType;
import de.hshannover.f4.trust.ifmapcli.common.enums.Significance;
import de.hshannover.f4.trust.ifmapcli.common.enums.WlanSecurityEnum;
import de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityType;

public class IfmapjEnumConverter {
	public static de.hshannover.f4.trust.ifmapj.metadata.EventType ifmapjEventTypeFrom(
			EventType type) {
		switch (type) {
		case p2p:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.p2p;
		case cve:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.cve;
		case botnet_infection:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.botnetInfection;
		case worm_infection:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.wormInfection;
		case excessive_flows:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.excessiveFlows;
		case behavioral_change:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.behavioralChange;
		case policy_violation:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.policyViolation;
		case other:
			return de.hshannover.f4.trust.ifmapj.metadata.EventType.other;
		default:
			throw new RuntimeException("unknown event type '" + type + "'");
		}
	}
	
	public static de.hshannover.f4.trust.ifmapj.metadata.Significance ifmapjSignificanceFrom(Significance type) {
		switch (type) {
		case critical:
				return de.hshannover.f4.trust.ifmapj.metadata.Significance.critical;
		case informational:
			return de.hshannover.f4.trust.ifmapj.metadata.Significance.informational;
		case important:
			return de.hshannover.f4.trust.ifmapj.metadata.Significance.important;
		default:
			throw new RuntimeException("unkown significance '" + type + "'");
		}
	}

	public static de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum ifmapjWlanSecurityTypeFrom(
			WlanSecurityEnum type) {
		switch (type) {
		case open:
			return de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum.open;
		case wep:
			return de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum.wep;
		case tkip:
			return de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum.tkip;
		case bip:
			return de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum.bip;
		case ccmp:
			return de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum.ccmp;
		case other:
			return de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum.other;
		default:
			throw new RuntimeException("unknown wlan security type '" + type + "'");
		}
	}
	
	public static de.hshannover.f4.trust.ifmapj.metadata.EnforcementAction ifmapjEnforcementActionFrom(EnforcementAction type) {
		switch (type) {
		case block:
				return de.hshannover.f4.trust.ifmapj.metadata.EnforcementAction.block;
		case quarantine:
			return de.hshannover.f4.trust.ifmapj.metadata.EnforcementAction.quarantine;
		case other:
			return de.hshannover.f4.trust.ifmapj.metadata.EnforcementAction.other;
		default:
			throw new RuntimeException("unkown enforcement action '" + type + "'");
		}
	}

	public static List<de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityType> ifmapjWlanSecurityTypeListFrom(
			List<WlanSecurityEnum> list) {
		List<de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityType> result = new ArrayList<de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityType>();
		
		for (WlanSecurityEnum element : list) {
			de.hshannover.f4.trust.ifmapj.metadata.WlanSecurityEnum type = ifmapjWlanSecurityTypeFrom(element);
			result.add(new WlanSecurityType(type, null));	// TODO build other-type-definition correctly
		}
		
		return result;
	}
}
