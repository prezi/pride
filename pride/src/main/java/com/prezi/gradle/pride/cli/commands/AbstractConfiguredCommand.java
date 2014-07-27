package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.DefaultRuntimeConfiguration;
import io.airlift.command.Option;
import org.apache.commons.configuration.PropertiesConfiguration;

import static com.prezi.gradle.pride.cli.Configurations.GRADLE_HOME;
import static com.prezi.gradle.pride.cli.Configurations.GRADLE_VERSION;

public abstract class AbstractConfiguredCommand extends AbstractCommand {
	@Option(name = "--gradle-version",
			title = "version",
			description = "Use specified Gradle version (can be a version number, a URL of a distribution, or a location of an installation)")
	private String explicitGradleVersion;

	@Option(name = "--gradle-home",
			title = "directory",
			description = "Use specified Gradle home")
	private String explicitGradleHome;

	@Override
	final public Integer call() throws Exception {
		PropertiesConfiguration globalConfiguration = loadGlobalConfiguration();
		RuntimeConfiguration config = DefaultRuntimeConfiguration.create(globalConfiguration);

		config.override(GRADLE_VERSION, explicitGradleVersion);
		config.override(GRADLE_HOME, explicitGradleHome);

		return executeWithConfiguration(config);
	}

	abstract protected int executeWithConfiguration(RuntimeConfiguration config) throws Exception;
}
