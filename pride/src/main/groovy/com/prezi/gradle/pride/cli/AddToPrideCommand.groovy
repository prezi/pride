package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.PrideInitializer
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "add", description = "Add modules to a pride")
class AddToPrideCommand extends PrideCommand {

	@Option(name = ["-o", "--overwrite"],
			description = "Overwrite existing modules in the pride")
	private boolean overwrite

	@Option(name = ["-b", "--base-url"],
			title = "url",
			description = "Base URL for Git repositories")
	private String explicitReposBaseUrl

	@Arguments(required = true, description = "Modules to add to the pride")
	private List<String> modules

	@Override
	void run() {
		// Check if anything exists already
		if (!overwrite) {
			def existingRepos = modules.findAll { new File(prideDirectory, it).exists() }
			if (existingRepos) {
				throw new PrideException("These modules already exist in pride: ${existingRepos.join(", ")}")
			}
		}

		// Clone repositories
		modules.each { moduleName ->
			def repository = reposBaseUrl + moduleName + ".git"
			def targetDirectory = new File(prideDirectory, moduleName)
			targetDirectory.deleteDir()

			System.out.println("Cloning ${repository}")
			def process = ["git", "clone", repository, targetDirectory].execute()
			process.waitForProcessOutput((OutputStream) System.out, System.err)
			if (process.exitValue()) {
				throw new PrideException("Could not clone ${targetDirectory}")
			}
		}

		// Re-initialize pride
		PrideInitializer.initializePride(prideDirectory, true)
	}

	private String getReposBaseUrl() {
		String reposBaseUrl = explicitReposBaseUrl ?: configuration.reposBaseUrl
		if (reposBaseUrl == null) {
			throw new PrideException("Base URL for Git repos is not set. Either specify via --base-url, " +
					"or set it in the global configuration -- see pride help config.")
		}
		if (!reposBaseUrl.endsWith("/")) {
			reposBaseUrl += "/"
		}
		return reposBaseUrl
	}
}
