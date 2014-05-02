package com.prezi.gradle.pride.cli

import org.apache.commons.configuration.MapConfiguration

/**
 * Created by lptr on 02/05/14.
 */
class CliConfiguration {
	public static final PRIDE_HOME = "pride.home"
	public static final REPO_TYPE_DEFAULT = "repo.type.default"
	public static final REPO_BASE_URL = "repo.base.url"
	public static final REPO_CACHE_ALWAYS = "repo.cache.always"

	static class Defaults extends MapConfiguration {
		public Defaults() {
			super([:])
			setProperty(PRIDE_HOME, System.getProperty("PRIDE_HOME") ?: System.getProperty("user.home") + "/.pride")
			setProperty(REPO_TYPE_DEFAULT, "git")
			setProperty(REPO_CACHE_ALWAYS, true)
		}
	}
}
