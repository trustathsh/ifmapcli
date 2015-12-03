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
 * This file is part of ifmapcli (ex-ident), version 0.3.1, implemented by the Trust@HsH
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import net.sourceforge.argparse4j.inf.ArgumentParser;

import org.w3c.dom.Document;

import util.DomHelpers;
import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapcli.common.enums.IdType;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.MarshalException;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;

/**
 * A simple tool that publishes or deletes extended-identifier. When metadata is
 * published, the lifetime is set to be 'forever'.
 * 
 * @author Marius Rohde
 * 
 */
public class ExIdent extends AbstractClient {

	/**
	 * 
	 * Method to initialize the parser operations
	 * 
	 * @param args
	 *            the arguments submitted to the cli module
	 * 
	 */
	private static void initParser(String[] args) {

		command = "ex-id";

		// ---parsing-------
		ArgumentParser parser = createDefaultParser();

		ParserUtil.addPublishOperation(parser);

		ParserUtil.addExIdentifier(parser);

		ParserUtil.addSecIdentifierType(parser, IdType.ipv4, IdType.ipv6,
				IdType.mac, IdType.dev, IdType.ar, IdType.id, IdType.id_aik,
				IdType.id_dist, IdType.id_dns, IdType.id_email,
				IdType.id_hiphit, IdType.id_kerberos, IdType.id_sip,
				IdType.id_tel, IdType.id_user, IdType.exid);
		ParserUtil.addSecIdentifier(parser);

		ParserUtil.addMetaFileInSystemIn(parser);

		parseParameters(parser, args);
	}

	/**
	 * Method to start this module
	 * 
	 */

	public static void main(String[] args) {

		initParser(args);

		printParameters(KEY_OPERATION, new String[] { KEY_EX_IDENTIFIER,
				KEY_SEC_IDENTIFIER_TYPE, KEY_SEC_IDENTIFIER,
				KEY_META_FILE_IN_SYSTEM_IN });

		// prepare identifiers

		File exIdent = resource.get(KEY_EX_IDENTIFIER);
		Identifier identifier1 = null;

		try {
			identifier1 = Identifiers
					.createExtendedIdentity(new FileInputStream(exIdent));
		} catch (MarshalException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		IdType identifierType2 = resource.get(KEY_SEC_IDENTIFIER_TYPE);
		String identifierNameOrFileName2 = resource
				.getString(KEY_SEC_IDENTIFIER);
		Identifier identifier2 = null;

		if (identifierType2 == null && identifierNameOrFileName2 != null) {
			throw new RuntimeException(
					"no identifier type specified for given identifier: "
							+ identifierNameOrFileName2);
		} else if (identifierType2 != null && identifierNameOrFileName2 == null) {
			throw new RuntimeException(
					"no identifier specified for given identifier type: "
							+ identifierType2);
		} else if (identifierType2 != null && identifierNameOrFileName2 != null) {
			if (identifierType2 == IdType.exid) {
				try {
					identifier2 = Identifiers
							.createExtendedIdentity(new FileInputStream(
									identifierNameOrFileName2));
				} catch (MarshalException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				identifier2 = getIdentifier(identifierType2,
						identifierNameOrFileName2);
			}
		}

		// prepare metadata

		File metadataXmlFile = resource.get(KEY_META_FILE_IN_SYSTEM_IN);
		Document metadata = readFileInSystemIn(metadataXmlFile);

		// Publish/delete the metadata
		publishDeleteMetaData(identifier1, identifier2, metadata);
	}

	/**
	 * 
	 * Helper method to read inputstream of file or system in
	 * 
	 * @param input
	 *            A File Object specifying System in or a File
	 * 
	 * @return A XML Metadata Document
	 */
	private static Document readFileInSystemIn(File input) {
		Document metadata = null;

		if (input.getName().equals("-")) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));

			// Workaround for multiline system in
			String line = "";
			try {
				line = br.readLine();
				while (br.ready()) {
					line += br.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			// ----------

			try {
				metadata = DomHelpers.toDocument(line, null);
			} catch (MarshalException e) {
				e.printStackTrace();
			}
		} else {
			try {
				metadata = DomHelpers.toDocument(new FileInputStream(input));
			} catch (MarshalException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		return metadata;
	}

	/**
	 * 
	 * helper Method to publish/delete the metadata
	 * 
	 * @param identifier1
	 *            the identifier1
	 * @param identifier2
	 *            the optional identifier2 must be null if not exist
	 * @param metadata
	 *            the metadata xml document
	 * 
	 */
	private static void publishDeleteMetaData(Identifier identifier1,
			Identifier identifier2, Document metadata) {

		String nsPrefix = metadata.getChildNodes().item(0).getPrefix();
		String nsUri = metadata.getChildNodes().item(0).getNamespaceURI();
		String elementname = metadata.getChildNodes().item(0).getLocalName();

		try {
			SSRC ssrc = createSSRC();
			ssrc.newSession();

			PublishRequest request;

			if (isUpdate(KEY_OPERATION)) {
				if (identifier2 == null) {
					PublishUpdate publishUpdate = Requests.createPublishUpdate(
							identifier1, metadata, MetadataLifetime.forever);
					request = Requests.createPublishReq(publishUpdate);
				} else {
					PublishUpdate publishUpdate = Requests.createPublishUpdate(
							identifier1, identifier2, metadata,
							MetadataLifetime.forever);
					request = Requests.createPublishReq(publishUpdate);
				}
			} else {
				PublishDelete publishDelete;
				String filter = String.format(nsPrefix + ":" + elementname
						+ "[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
				if (identifier2 == null) {
					publishDelete = Requests.createPublishDelete(identifier1,
							filter);
				} else {
					publishDelete = Requests.createPublishDelete(identifier1,
							identifier2, filter);
				}
				publishDelete.addNamespaceDeclaration(nsPrefix, nsUri);
				request = Requests.createPublishReq(publishDelete);
			}

			ssrc.publish(request);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

}
