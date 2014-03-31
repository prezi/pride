package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.SessionInitializer
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "remove", description = "Remove modules from a session")
class RemoveFromSession extends SessionCommand {

	@Option(name = ["-f", "--force"],
			description = "Remove modules even if there are local changes")
	private boolean force

	@Arguments(required = true, description = "Modules to remove from the session")
	private List<String> modules

	@Override
	void run() {
		// Check if anything exists already
		if (!force) {
			def missingModules = modules.findAll { module -> !new File(sessionDirectory, module).exists() }
			if (missingModules) {
				throw new PrideException("These modules are missing: ${missingModules}")
			}
			def changedModules = modules.findAll { module ->
				def moduleDir = new File(sessionDirectory, module)
				def process = ["git", "--git-dir=${moduleDir}/.git", "--work-tree=${moduleDir}", "status", "--porcelain"].execute()
				process.waitFor()
				if (process.exitValue()) {
					throw new PrideException("Git status failed:\n" + process.text)
				}
				return !process.text.trim().empty
			}
			if (changedModules) {
				throw new PrideException("These modules have uncommitted changes: ${changedModules.join(", ")}")
			}
		}

		// Remove modules
		modules.each { module ->
			def moduleDir = new File(sessionDirectory, module)
			System.out.println("Removing ${moduleDir}")
			moduleDir.deleteDir()
		}

		// Re-initialize session
		SessionInitializer.initializeSession(sessionDirectory, true)
	}
}
