package com.prezi.gradle.pride.cli

/**
 * Created by lptr on 15/04/14.
 */
class Configuration {
	private static final REPOS_BASE_URL = "repos.base.url"

	private final Properties configuration
	private final File configFile

	public Configuration() {
		this.configuration = new Properties()
		this.configFile = new File("${System.getProperty("user.home")}/.prideconfig")
		load()
	}

	public String getReposBaseUrl() {
		return configuration.getProperty(REPOS_BASE_URL)
	}

	public void setReposBaseUrl(String reposBaseUrl) {
		configuration.setProperty(REPOS_BASE_URL, reposBaseUrl)
	}

	public void load() {
		configuration.clear()
		if (configFile.exists()) {
			configFile.withReader { configuration.load(it) }
		}
	}

	public void save() {
		configFile.delete()
		configFile.withWriter { configuration.store(it, null) }
	}
}
