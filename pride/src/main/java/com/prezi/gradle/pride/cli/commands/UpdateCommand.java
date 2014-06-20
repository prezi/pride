package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Command(name = "update", description = "Updates a pride")
public class UpdateCommand extends AbstractExistingPrideCommand {

	@Option(name = "--exclude",
			title = "module",
			description = "Do not execute command on module (can be specified multiple times)")
	private List<String> excludeModules;

	@Arguments(required = false,
			title = "modules",
			description = "The modules to update (updates all modules if none specified)")
	private List<String> includeModules;

	@Override
	public void runInPride(final Pride pride) throws IOException {
		for (Module module : pride.filterModules(includeModules, excludeModules)) {
			logger.info("Updating " + module.getName());
			File moduleDir = pride.getModuleDirectory(module.getName());
			module.getVcs().getSupport().update(moduleDir, false);
		}
	}
}
