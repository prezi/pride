package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.ProcessUtils
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "remove", description = "Remove modules from a pride")
class RemoveCommand extends AbstractExistingPrideCommand {
	@Option(name = ["-f", "--force"],
			description = "Remove modules even if there are local changes")
	private boolean force

	@Arguments(required = true, description = "Modules to remove from the pride")
	private List<String> modulesNames

	@Override
	void runInPride(Pride pride) {
		// Check if anything exists already
		if (!force) {
			def missingModules = modulesNames.findAll { !pride.hasModule(it) }
			if (missingModules) {
				throw new PrideException("These modules are missing: ${missingModules*.name}")
			}
			def changedModules = modulesNames.findAll { moduleName ->
				def moduleDir = pride.getModuleDirectory(moduleName)
				def process = ProcessUtils.executeIn(moduleDir, ["git", "status", "--porcelain"], false)
				return !process.text.trim().empty
			}
			if (changedModules) {
				throw new PrideException("These modules have uncommitted changes: ${changedModules.join(", ")}")
			}
		}

		// Remove modules
		modulesNames.each { moduleName ->
			pride.removeModule(moduleName)
		}
		pride.save()

		// Re-initialize pride
		pride.reinitialize()
	}
}

/**
 * Created by lptr on 16/04/14.
 */
@Command(name = "rm",
		hidden = true,
		description = "Remove modules from a pride")
class RmCommandAlias extends RemoveCommand {
}
