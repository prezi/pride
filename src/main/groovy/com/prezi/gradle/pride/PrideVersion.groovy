package com.prezi.gradle.pride

/**
 * Created by lptr on 01/04/14.
 */
final class PrideVersion {
	public static final String VERSION = loadVersion()

	private static String loadVersion() {
		def props = new Properties()
		props.load(getClass().getResourceAsStream("/version.properties"))
		return props["application.version"]
	}
}
