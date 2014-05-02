package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.cli.CliConfiguration
import com.prezi.gradle.pride.vcs.RepoCache
import io.airlift.command.Option
import io.airlift.command.OptionType
import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.FileConfiguration
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 15/04/14.
 */
abstract class AbstractCommand implements Runnable {
	protected static final Logger log = LoggerFactory.getLogger(AbstractCommand)

	@Option(type = OptionType.GLOBAL,
			name = ["-v", "--verbose"],
			description = "Verbose mode")
	public boolean verbose

	@Option(type = OptionType.GLOBAL,
			name = ["-q", "--quiet"],
			description = "Quite mode")
	public boolean quiet

	protected final FileConfiguration fileConfiguration = loadConfiguration()

	private static PropertiesConfiguration loadConfiguration() {
		def configFile = new File("${System.getProperty("user.home")}/.prideconfig")
		if (!configFile.exists()) {
			configFile.parentFile.mkdirs()
			configFile.createNewFile()
		}
		return new PropertiesConfiguration(configFile)
	}

	private CompositeConfiguration processedConfiguration
	protected final Configuration getConfiguration() {
		if (processedConfiguration == null) {
			processedConfiguration = new CompositeConfiguration([fileConfiguration, new CliConfiguration.Defaults()])
			overrideConfiguration(processedConfiguration.inMemoryConfiguration)
		}
		return processedConfiguration
	}

	@SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
	protected void overrideConfiguration(Configuration configuration) {
	}

	private RepoCache repoCache
	protected RepoCache getRepoCache() {
		if (repoCache == null) {
			def cachePath = new File(configuration.getString(CliConfiguration.PRIDE_HOME) + "/cache")
			repoCache = new RepoCache(cachePath)
		}
		return repoCache
	}

	protected String getRepoBaseUrl() {
		String repoBaseUrl = configuration.getString(CliConfiguration.REPO_BASE_URL)
		if (repoBaseUrl == null) {
			throw invalidOptionException("You have specified a module name, but base URL for Git repos is not set",
					"a full repository URL, specify the base URL via --repo-base-url", CliConfiguration.REPO_BASE_URL)
		}
		if (!repoBaseUrl.endsWith("/")) {
			repoBaseUrl += "/"
		}
		return repoBaseUrl
	}

	protected static PrideException invalidOptionException(String message, String option, String configuration) {
		return new PrideException("${message}. Either use ${option}, or set it in the global configuration (~/.prideconfig) as \"${configuration}\". See 'pride help config' for more information.")
	}
}
