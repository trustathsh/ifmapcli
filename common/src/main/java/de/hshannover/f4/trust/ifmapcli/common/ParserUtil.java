package de.hshannover.f4.trust.ifmapcli.common;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;

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
		if (url == null) url = DefaultConfig.DEFAULT_URL;
		if (user == null) user = DefaultConfig.DEFAULT_USER;
		if (pass == null) pass = DefaultConfig.DEFAULT_PASS;
		if (keystorePath == null) keystorePath = DefaultConfig.DEFAULT_KEYSTORE_PATH;
		if (keystorePass == null) keystorePass = DefaultConfig.DEFAULT_KEYSTORE_PASS;

		parser.addArgument("--url")
			.type(String.class)
			.dest(URL)
			.setDefault(url)
			.help("the MAP server URL");
		parser.addArgument("--user")
			.type(String.class)
			.dest(USER)
			.setDefault(user)
			.help("IF-MAP basic auth user");
		parser.addArgument("--pass")
			.type(String.class)
			.dest(PASS)
			.setDefault(pass)
			.help("user password");
		parser.addArgument("--keystore-path")
			.type(String.class)
			.dest(KEYSTORE_PATH)
			.setDefault(keystorePath)
			.help("the keystore file");
		parser.addArgument("--keystore-pass")
			.type(String.class)
			.dest(KEYSTORE_PASS)
			.setDefault(keystorePass)
			.help("password for the keystore");
	}

	public static void addCommonArgumentsTo(ArgumentParser parser) {
		parser.addArgument("-v")
			.type(Boolean.class)
			.action(Arguments.storeTrue())
			.dest(VERBOSE)
			.setDefault(false)
			.help("print logging information");
	}

}
