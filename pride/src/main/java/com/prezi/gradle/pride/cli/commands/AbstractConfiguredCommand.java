package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Strings;
import com.prezi.gradle.pride.cli.CliConfiguration;
import io.airlift.command.Option;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.util.Arrays;

public abstract class AbstractConfiguredCommand extends AbstractCommand {
	@Option(name = "--gradle-version",
			title = "version",
			description = "Use specified Gradle version")
	private String explicitGradleVersion;

	@Override
	final public Integer call() throws Exception {
		PropertiesConfiguration globalConfiguration = loadGlobalConfiguration();
		Configuration config = new CompositeConfiguration(Arrays.<Configuration> asList(globalConfiguration, new CliConfiguration.Defaults()));
		return executeWithConfiguration(config);
	}

	abstract protected int executeWithConfiguration(Configuration globalConfig) throws Exception;

	protected String getGradleVersion(Configuration config) {
		return override(config, CliConfiguration.GRADLE_VERSION, explicitGradleVersion);
	}

	protected static boolean override(Configuration config, String property, Boolean override) {
		if (override != null) {
			config.setProperty(property, override);
		}
		return config.getBoolean(property);
	}

	protected static boolean override(Configuration config, String property, boolean overrideEnabled, boolean overrideDisabled) {
		if (overrideEnabled) {
			config.setProperty(property, true);
		} else if (overrideDisabled) {
			config.setProperty(property, false);
		}
		return config.getBoolean(property);
	}

	protected static String override(Configuration config, String property, String override) {
		if (!Strings.isNullOrEmpty(override)) {
			config.setProperty(property, override);
		}
		return config.getString(property);
	}
}
