package com.prezi.gradle.pride.cli

/**
 * Created by lptr on 15/04/14.
 */
class Configuration {
	private static final REPO_BASE_URL = "repo.base.url"
	private static final REPO_CACHE_PATH = "repo.cache.path"
	private static final REPO_CACHE_ALWAYS = "repo.cache.always"

	private final Properties configuration
	private final File configFile

	public Configuration() {
		this.configuration = new Properties()
		this.configFile = new File("${System.getProperty("user.home")}/.prideconfig")
		load()
	}

	public String getRepoBaseUrl() {
		return configuration.getProperty(REPO_BASE_URL)
	}

	public void setRepoBaseUrl(String repoBaseUrl) {
		configuration.setProperty(REPO_BASE_URL, repoBaseUrl)
	}

	public File getRepoCachePath() {
		def path = configuration.getProperty(REPO_CACHE_PATH)
		if (path == null) {
			path = "${System.getProperty("user.home")}/.pride/cache"
		}
		return new File(path)
	}

	public String setRepoCachePath(File repoCachePath) {
		configuration.setProperty(REPO_CACHE_PATH, repoCachePath.toString())
	}

	public boolean getRepoCacheAlways() {
		return configuration.getProperty(REPO_CACHE_ALWAYS) != "false"
	}

	public void setRepoCacheAlways(boolean repoCacheAlways) {
		configuration.setProperty(REPO_CACHE_ALWAYS, String.valueOf(repoCacheAlways))
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
