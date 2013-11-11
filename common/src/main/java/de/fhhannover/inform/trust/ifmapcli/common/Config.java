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
 * This file is part of Ifmapcli, version 0.0.4, implemented by the Trust@FHH 
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

/**
 * Holds the configuration parameters for a CLI tool.
 * 
 * @author ib
 *
 */
public class Config {
	
	public static final String DEFAULT_URL = "https://localhost:8443";
	public static final String DEFAULT_USER = "test";
	public static final String DEFAULT_PASS = "test";
	public static final String DEFAULT_TRUSTSTORE_PATH = "/ifmapcli.jks";
	public static final String DEFAULT_TRUSTSTORE_PASS = "ifmapcli";
	
	private String mUrl;
	private String mUser;
	private String mPass;
	private String mTruststorePath;
	private String mTruststorePass;
	
	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String url) {
		mUrl = url;
	}

	public String getUser() {
		return mUser;
	}

	public void setUser(String user) {
		this.mUser = user;
	}

	public String getPass() {
		return mPass;
	}

	public void setPass(String pass) {
		this.mPass = pass;
	}

	public String getTruststorePath() {
		return mTruststorePath;
	}

	public void setTruststorePath(String truststorePath) {
		this.mTruststorePath = truststorePath;
	}
	
	public String getTruststorePass() {
		return mTruststorePass;
	}

	public void setTruststorePass(String truststorePass) {
		this.mTruststorePass = truststorePass;
	}

	public Config() {
		this(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASS, DEFAULT_TRUSTSTORE_PATH, DEFAULT_TRUSTSTORE_PASS);
	}
	
	public Config(String url, String user, String pass, String keystorePath, String keystorePass) {
		setUrl(url);
		setUser(user);
		setPass(pass);
		setTruststorePath(keystorePath);
		setTruststorePass(keystorePass);
	}
	
	@Override
	public String toString() {
		return "url:" + mUrl +
				" user:" + mUser +
				" pass:" + mPass +
				" truststorePath:" + mTruststorePath +
				" truststorePass:" + mTruststorePass;
	}
	
}
