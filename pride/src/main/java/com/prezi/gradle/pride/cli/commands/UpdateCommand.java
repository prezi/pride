package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.cli.CliConfiguration;
import com.prezi.gradle.pride.cli.PrideInitializer;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Command(name = "update", description = "Updates a pride")
public class UpdateCommand extends AbstractExistingPrideCommand {

	@Option(name = "--exclude",
			title = "module",
			description = "Do not execute command on module (can be specified multiple times)")
	private List<String> excludeModules;

	@Option(name = {"-D", "--refresh-dependencies"},
			description = "Refresh Gradle dependencies after update completed")
	private Boolean explicitRefreshDependencies;

	@Arguments(required = false,
			title = "modules",
			description = "The modules to update (updates all modules if none specified)")
	private List<String> includeModules;

	@Override
	protected void overrideConfiguration(Configuration configuration) {
		super.overrideConfiguration(configuration);
		if (explicitRefreshDependencies != null) {
			configuration.setProperty(CliConfiguration.COMMAND_UPDATE_REFRESH_DEPENDENCIES, explicitRefreshDependencies);
		}
	}

	@Override
	public void runInPride(final Pride pride) throws IOException {
		for (Module module : pride.filterModules(includeModules, excludeModules)) {
			logger.info("Updating " + module.getName());
			File moduleDir = pride.getModuleDirectory(module.getName());
			module.getVcs().getSupport().update(moduleDir, false);
		}

		if (getConfiguration().getBoolean(CliConfiguration.COMMAND_UPDATE_REFRESH_DEPENDENCIES)) {
			PrideInitializer.refreshDependencies(pride);
		}
	}
}
