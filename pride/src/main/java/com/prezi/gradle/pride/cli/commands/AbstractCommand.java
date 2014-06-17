package com.prezi.gradle.pride.cli.commands;

import com.google.common.collect.Lists;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.CliConfiguration;
import com.prezi.gradle.pride.vcs.RepoCache;
import io.airlift.command.Option;
import io.airlift.command.OptionType;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public abstract class AbstractCommand implements Runnable {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractCommand.class);
	@Option(type = OptionType.GLOBAL, name = {"-v", "--verbose"}, description = "Verbose mode")
	public boolean verbose;
	@Option(type = OptionType.GLOBAL, name = {"-q", "--quiet"}, description = "Quite mode")
	public boolean quiet;
	protected final FileConfiguration fileConfiguration = loadConfiguration();
	private CompositeConfiguration processedConfiguration;
	private RepoCache repoCache;

	@Override
	final public void run() {
		try {
			runInternal();
		} catch (PrideException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected abstract void runInternal() throws IOException;

	private static PropertiesConfiguration loadConfiguration() {
		try {
			File configFile = new File(System.getProperty("user.home") + "/.prideconfig");
			if (!configFile.exists()) {
				configFile.getParentFile().mkdirs();
				configFile.createNewFile();
			}

			return new PropertiesConfiguration(configFile);
		} catch (Exception ex) {
			throw new RuntimeException("Couldn't load configuration file", ex);
		}
	}

	protected final Configuration getConfiguration() {
		if (processedConfiguration == null) {
			processedConfiguration = new CompositeConfiguration(Lists.newArrayList(fileConfiguration, new CliConfiguration.Defaults()));
			overrideConfiguration(processedConfiguration);
		}

		return processedConfiguration;
	}

	protected void overrideConfiguration(Configuration configuration) {
	}

	protected RepoCache getRepoCache() throws IOException {
		if (repoCache == null) {
			File cachePath = new File(getConfiguration().getString(CliConfiguration.PRIDE_HOME) + "/cache");
			repoCache = new RepoCache(cachePath);
		}

		return repoCache;
	}

	protected String getRepoBaseUrl() {
		String repoBaseUrl = getConfiguration().getString(CliConfiguration.REPO_BASE_URL);
		if (repoBaseUrl == null) {
			throw invalidOptionException("You have specified a module name, but base URL for Git repos is not set", "a full repository URL, specify the base URL via --repo-base-url", CliConfiguration.REPO_BASE_URL);
		}

		if (!repoBaseUrl.endsWith("/")) {
			repoBaseUrl += "/";
		}

		return repoBaseUrl;
	}

	protected static PrideException invalidOptionException(final String message, final String option, final String configuration) {
		return new PrideException(message + ". Either use " + option + ", or set it in the global configuration (~/.prideconfig) as \"" + configuration + "\". See \'pride help config\' for more information.");
	}
}
