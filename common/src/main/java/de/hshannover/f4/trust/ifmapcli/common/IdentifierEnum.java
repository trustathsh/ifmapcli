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
 * This file is part of ifmapcli (common), version 0.0.6, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2013 Trust@HsH
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

import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.identifier.IdentityType;


/**
 * Enum for all standard identifier types (except other). Furthermore,
 * only type=username is supported for identity identifiers.
 *
 * Includes methods in order to avoid switch/case statements during
 * identifier creation.
 *
 * @author ib
 *
 */
public enum IdentifierEnum {
	ip {
		@Override
		public Identifier getIdentifier(String value) {
			return Identifiers.createIp4(value);
		}
	},
	mac {
		@Override
		public Identifier getIdentifier(String value) {
			return Identifiers.createMac(value);
		}
	},
	ar {
		@Override
		public Identifier getIdentifier(String value) {
			return Identifiers.createAr(value);
		}
	},
	dev {
		@Override
		public Identifier getIdentifier(String value) {
			return Identifiers.createDev(value);
		}
	},
	id {
		@Override
		public Identifier getIdentifier(String value) {
			return Identifiers.createIdentity(
					IdentityType.userName, value);
		}
	};

	public Identifier getIdentifier(String value) {
		return null;
	}

}
