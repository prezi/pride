package com.prezi.gradle.pride;

import java.io.IOException;
import java.util.Properties;

public final class PrideVersion {

	public static final String VERSION = loadVersion();

	private static String loadVersion() {
		Properties props = new Properties();
		try {
			props.load(PrideVersion.class.getResourceAsStream("/version.properties"));
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}
		return (String) props.get("application.version");
	}
}
