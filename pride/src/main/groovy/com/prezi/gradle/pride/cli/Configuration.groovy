package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 15/04/14.
 */
class Configuration {
	private static final Logger log = LoggerFactory.getLogger(Configuration)

	public static final REPO_TYPE_DEFAULT = "repo.type.default"
	public static final REPO_BASE_URL = "repo.base.url"
	public static final REPO_CACHE_PATH = "repo.cache.path"
	public static final REPO_CACHE_ALWAYS = "repo.cache.always"

	private final Properties configuration
	private final File configFile

	public Configuration() {
		this.configuration = new Properties()
		this.configFile = new File("${System.getProperty("user.home")}/.prideconfig")
		load()
	}

	public String getRepoTypeDefault() {
		return configuration.getProperty(REPO_TYPE_DEFAULT)
	}

	public void setRepoTypeDefault(String repoTypeDefault) {
		// TODO Check if it's a supported repo type
		configuration.setProperty(REPO_TYPE_DEFAULT, repoTypeDefault)
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

	public String getParameter(String property) {
		switch (property) {
			case REPO_TYPE_DEFAULT:
				return repoTypeDefault
			case REPO_BASE_URL:
				return repoBaseUrl
			case REPO_CACHE_PATH:
				return repoCachePath
			case REPO_CACHE_ALWAYS:
				return repoCacheAlways
			default:
				throw new PrideException("Unknown configuration parameter ${property}")
		}
	}

	public void setParameter(String property, String value) {
		log.debug "Setting \"$property\" to \"$value\""
		switch (property) {
			case REPO_TYPE_DEFAULT:
				setRepoTypeDefault(value)
				break
			case REPO_BASE_URL:
				setRepoBaseUrl(value)
				break
			case REPO_CACHE_PATH:
				setRepoCachePath(new File(value))
				break
			case REPO_CACHE_ALWAYS:
				switch (value) {
					case "true":
						setRepoCacheAlways(true)
						break
					case "false":
						setRepoCacheAlways(false)
						break
					default:
						throw new PrideException("Can only set ${property} to \"true\" or \"false\"")
				}
				break
			default:
				throw new PrideException("Unknown configuration parameter ${property}")
		}
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
