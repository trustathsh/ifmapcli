package de.fhhannover.inform.trust.ifmapcli.common;

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
 * This file is part of Ifmapcli, version 0.0.5, implemented by the Trust@FHH 
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

import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IdentityType;


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
