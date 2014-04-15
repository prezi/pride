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

	@Option(name = ["-b", "--repo-base-url"],
			title = "url",
			description = "Base URL for Git repositories")
	private String explicitRepoBaseUrl

	@Option(name = ["-c", "--use-repo-cache"],
			description = "Use local repo cache")
	private boolean explicitUseRepoCache

	@Option(name = ["--no-repo-cache"],
			description = "Do not use local repo cache")
	private boolean explicitDontUseRepoCache

	@Option(name = "--repo-cache-path",
			title = "directory",
			description = "Local repo cache location")
	private File explicitRepoCachePath

	@Arguments(required = true, description = "Modules to add to the pride")
	private List<String> modules

	@Override
	void run() {
		def useRepoCache = explicitUseRepoCache || (!explicitDontUseRepoCache && configuration.repoCacheAlways)

		// Check if anything exists already
		if (!overwrite) {
			def existingRepos = modules.findAll { new File(prideDirectory, it).exists() }
			if (existingRepos) {
				throw new PrideException("These modules already exist in pride: ${existingRepos.join(", ")}")
			}
		}

		// Clone repositories
		modules.each { moduleName ->
			if (useRepoCache) {
				def moduleInCache = new File(repoCachePath, moduleName)
				def moduleInPride = new File(prideDirectory, moduleName)
				if (!moduleInCache.exists()) {
					moduleInCache = cloneRepository(moduleName, repoCachePath)
				}
				def process = ["ln", "-s", moduleInCache.absolutePath, moduleInPride.absolutePath].execute()
				process.waitForProcessOutput((OutputStream) System.out, System.err)
				if (process.exitValue()) {
					throw new PrideException("Could not link \"${moduleInCache}\" to \"${moduleInPride}")
				}
			} else {
				cloneRepository(moduleName, prideDirectory)
			}
		}

		// Re-initialize pride
		PrideInitializer.initializePride(prideDirectory, true)
	}

	protected File cloneRepository(String moduleName, File reposDir) {
		def repository = repoBaseUrl + moduleName + ".git"
		def targetDirectory = new File(reposDir, moduleName)
		reposDir.mkdirs()
		// Make sure we delete symlinks and directories alike
		targetDirectory.delete() || targetDirectory.deleteDir()

		System.out.println("Cloning ${repository}")
		def process = ["git", "clone", repository, targetDirectory].execute()
		process.waitForProcessOutput((OutputStream) System.out, System.err)
		if (process.exitValue()) {
			throw new PrideException("Could not clone ${targetDirectory}")
		}
		return targetDirectory
	}

	private String getRepoBaseUrl() {
		String repoBaseUrl = explicitRepoBaseUrl ?: configuration.repoBaseUrl
		if (repoBaseUrl == null) {
			throw new PrideException("Base URL for Git repos is not set. Either specify via --base-url, " +
					"or set it in the global configuration -- see pride help config.")
		}
		if (!repoBaseUrl.endsWith("/")) {
			repoBaseUrl += "/"
		}
		return repoBaseUrl
	}

	private File getRepoCachePath() {
		def path = explicitRepoCachePath ?: configuration.repoCachePath
		if (!path) {
			throw new PrideException("Repo cache path is not set. Either use --repo-cache-path, or set it in ~/.prideconfig.")
		}
		return path
	}
}
