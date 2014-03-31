package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.SessionInitializer
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "add", description = "Add modules to a session")
class AddToSession extends SessionCommand {

	@Option(name = ["-o", "--overwrite"],
			description = "Overwrite existing modules in the session")
	private boolean overwrite

	@Arguments(required = true, description = "Modules to add to the session")
	private List<String> modules

	@Override
	void run() {
		// Check if anything exists already
		if (!overwrite) {
			def existingRepos = modules.findAll { new File(sessionDirectory, it).exists() }
			if (existingRepos) {
				throw new PrideException("These modules already exist in session: ${existingRepos.join(", ")}")
			}
		}

		// Clone repositories
		modules.each { moduleName ->
			def repository = "git@github.com:prezi/${moduleName}.git"
			def targetDirectory = new File(sessionDirectory, moduleName)
			targetDirectory.deleteDir()

			System.out.println("Cloning ${repository}")
			def process = ["git", "clone", repository, targetDirectory].execute()
			process.waitForProcessOutput((OutputStream) System.out, System.err)
			if (process.exitValue()) {
				throw new PrideException("Could not clone ${targetDirectory}")
			}
		}

		// Re-initialize session
		SessionInitializer.initializeSession(sessionDirectory, true)
	}
}
