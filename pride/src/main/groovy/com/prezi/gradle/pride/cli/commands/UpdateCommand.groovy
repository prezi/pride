package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Module
import com.prezi.gradle.pride.Pride
import io.airlift.command.Arguments
import io.airlift.command.Command

/**
 * Created by lptr on 29/04/14.
 */
@Command(name = "update", description = "Updates a pride")
class UpdateCommand extends AbstractExistingPrideCommand {

	@Arguments(required = false,
			title = "modules",
			description = "The modules to update (updates all modules if none specified)")
	private List<String> modules

	@Override
	void runInPride(Pride pride) {
		Collection<Module> modulesToUpdate
		if (!modules) {
			modulesToUpdate = pride.modules
		} else {
			modulesToUpdate = modules.collect { pride.getModule(it) }
		}
		modulesToUpdate.each { Module module ->
			log.info("Updating ${module.name}")
			def moduleDir = pride.getModuleDirectory(module.name)
			module.vcs.support.update(moduleDir, false)
		}
	}
}
