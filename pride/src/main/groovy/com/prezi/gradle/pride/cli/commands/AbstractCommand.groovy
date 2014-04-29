package com.prezi.gradle.pride.cli.commands

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

	protected final FileConfiguration fileConfiguration = new PropertiesConfiguration("${System.getProperty("user.home")}/.prideconfig")

	private CompositeConfiguration processedConfiguration
	protected final Configuration getConfiguration() {
		if (processedConfiguration == null) {
			processedConfiguration = new CompositeConfiguration([fileConfiguration])
			overrideConfiguration(processedConfiguration.inMemoryConfiguration)
		}
		return processedConfiguration
	}

	@SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
	protected void overrideConfiguration(Configuration configuration) {
	}
}
