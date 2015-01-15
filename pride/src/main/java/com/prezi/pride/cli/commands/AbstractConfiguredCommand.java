package com.prezi.pride.cli.commands;

import com.prezi.pride.RuntimeConfiguration;
import com.prezi.pride.cli.DefaultRuntimeConfiguration;
import io.airlift.command.Option;
import org.apache.commons.configuration.PropertiesConfiguration;

import static com.prezi.pride.cli.Configurations.GRADLE_HOME;
import static com.prezi.pride.cli.Configurations.GRADLE_VERSION;

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

		executeWithConfiguration(config);
		return 0;
	}

	abstract protected void executeWithConfiguration(RuntimeConfiguration config) throws Exception;
}
