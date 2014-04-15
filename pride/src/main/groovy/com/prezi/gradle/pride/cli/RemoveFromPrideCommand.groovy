package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.PrideInitializer
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "remove", description = "Remove modules from a pride")
class RemoveFromPrideCommand extends PrideCommand {

	@Option(name = ["-f", "--force"],
			description = "Remove modules even if there are local changes")
	private boolean force

	@Arguments(required = true, description = "Modules to remove from the pride")
	private List<String> modules

	@Override
	void run() {
		// Check if anything exists already
		if (!force) {
			def missingModules = modules.findAll { module -> !new File(prideDirectory, module).exists() }
			if (missingModules) {
				throw new PrideException("These modules are missing: ${missingModules}")
			}
			def changedModules = modules.findAll { module ->
				def moduleDir = new File(prideDirectory, module)

				def commandLine = ["git", "--git-dir=${moduleDir}/.git", "--work-tree=${moduleDir}", "status", "--porcelain"]
				def process = commandLine.execute()
				process.waitFor()
				if (process.exitValue()) {
					throw new PrideException("Git status failed: ${commandLine.join(" ")}\n${process.text}")
				}
				return !process.text.trim().empty
			}
			if (changedModules) {
				throw new PrideException("These modules have uncommitted changes: ${changedModules.join(", ")}")
			}
		}

		// Remove modules
		modules.each { module ->
			def moduleDir = new File(prideDirectory, module)
			System.out.println("Removing ${moduleDir}")
			// Make sure we remove symlinks and directories alike
			moduleDir.delete() || moduleDir.deleteDir()
		}

		// Re-initialize pride
		PrideInitializer.initializePride(prideDirectory, true)
	}
}
