/**
 * 
 */
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
 * This file is part of Ifmapcli, version 0.0.3, implemented by the Trust@FHH 
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;

import org.junit.Test;

/**
 * Test class for {@link Common}
 * 
 * @author ib
 */
public class TestCommon {
	
	@Test
	public void testIsUpdate() {
		assertTrue(Common.isUpdate("update"));
		assertFalse(Common.isUpdate("Update"));
	}
	
	@Test
	public void testIsDelete() {
		assertTrue(Common.isDelete("delete"));
		assertFalse(Common.isDelete("Delete"));
	}
	
	@Test
	public void testIsUpdateOrDelete() {
		assertTrue(Common.isUpdateorDelete("update"));
		assertTrue(Common.isUpdateorDelete("delete"));
		assertFalse(Common.isUpdateorDelete("Update"));
		assertFalse(Common.isUpdateorDelete("Delete"));
	}
	
	@Test
	public void testGetTimeAsXsdDateTime() {
		Calendar gc = new GregorianCalendar(SimpleTimeZone.getTimeZone("CET"), Locale.GERMANY);
		gc.set(2011, 12, 01, 13, 37, 00);
		// FIXME this might be broken in different timezones
		assertEquals("2012-01-01T13:37:00+01:00", Common.getTimeAsXsdDateTime(gc.getTime()));
	}
	
}
