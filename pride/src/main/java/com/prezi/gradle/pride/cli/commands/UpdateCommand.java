package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.cli.PrideInitializer;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.prezi.gradle.pride.cli.CliConfiguration.COMMAND_UPDATE_REFRESH_DEPENDENCIES;
import static com.prezi.gradle.pride.cli.CliConfiguration.REPO_RECURSIVE;

@Command(name = "update", description = "Updates a pride")
public class UpdateCommand extends AbstractPrideCommand {

	@Option(name = "--exclude",
			title = "module",
			description = "Do not execute command on module (can be specified multiple times)")
	private List<String> excludeModules;

	@Option(name = {"-D", "--refresh-dependencies"},
			description = "Refresh Gradle dependencies after update completed")
	private Boolean explicitRefreshDependencies;

	@Option(name = {"-r", "--recursive"},
			description = "Update sub-modules recursively")
	private Boolean explicitRecursive;

	@Arguments(required = false,
			title = "modules",
			description = "The modules to update (updates all modules if none specified)")
	private List<String> includeModules;

	@Override
	public void executeInPride(Pride pride) throws IOException {
		Configuration config = pride.getConfiguration();
		boolean recursive = override(config, REPO_RECURSIVE, explicitRecursive);
		boolean refreshDependencies = override(config, COMMAND_UPDATE_REFRESH_DEPENDENCIES, explicitRefreshDependencies);

		for (Module module : pride.filterModules(includeModules, excludeModules)) {
			logger.info("Updating " + module.getName());
			File moduleDir = pride.getModuleDirectory(module.getName());
			module.getVcs().getSupport().update(moduleDir, recursive, false);
		}

		if (refreshDependencies) {
			new PrideInitializer(getGradleVersion(config)).refreshDependencies(pride);
		}
	}
}
