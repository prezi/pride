package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Command(name = "update", description = "Updates a pride")
public class UpdateCommand extends AbstractExistingPrideCommand {

	@Arguments(required = false,
			title = "modules",
			description = "The modules to update (updates all modules if none specified)")
	private List<String> modules;

	@Override
	public void runInPride(final Pride pride) throws IOException {
		Collection<Module> modulesToUpdate;
		if (modules.isEmpty()) {
			modulesToUpdate = pride.getModules();
		} else {
			modulesToUpdate = Collections2.transform(modules, new Function<String, Module>() {
				@Override
				public Module apply(String it) {
					return pride.getModule(it);
				}
			});
		}

		for (Module module : modulesToUpdate) {
			logger.info((String) "Updating " + module.getName());
			File moduleDir = pride.getModuleDirectory(module.getName());
			module.getVcs().getSupport().update(moduleDir, false);
		}
	}
}
