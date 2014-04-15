package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.PrideInitializer
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "add", description = "Add modules to a pride")
class AddToPrideCommand extends AbstractPrideCommand {
	private static final Logger log = LoggerFactory.getLogger(AddToPrideCommand)

	@Option(name = ["-o", "--overwrite"],
			description = "Overwrite existing modules in the pride")
	private boolean overwrite

	@Option(name = ["-B", "--repo-base-url"],
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
				if (!moduleInCache.exists()) {
					cloneModuleFromExternal(moduleName, repoCachePath, true)
				} else {
					log.info "Updating cached module in ${moduleInCache}"
					executeIn(moduleInCache, ["git", "fetch", "--all"])
				}
				def moduleInPride = cloneModuleFromCache(moduleName, prideDirectory)
				executeIn(moduleInPride, ["git", "remote", "set-url", "origin", getModuleRepoUrl(moduleName)])
			} else {
				cloneModuleFromExternal(moduleName, prideDirectory, false)
			}
		}

		// Re-initialize pride
		PrideInitializer.initializePride(prideDirectory, true)
	}

	private File cloneModuleFromCache(String moduleName, File reposDir) {
		def repository = new File(repoCachePath, moduleName).absolutePath
		cloneRepository(repository, moduleName, reposDir, false)
	}

	private File cloneModuleFromExternal(String moduleName, File reposDir, boolean mirror) {
		def repository = getModuleRepoUrl(moduleName)
		cloneRepository(repository, moduleName, reposDir, mirror)
	}

	private String getModuleRepoUrl(String moduleName) {
		return repoBaseUrl + moduleName + ".git"
	}

	private static File cloneRepository(String repository, String moduleName, File reposDir, boolean mirror) {
		def targetDirectory = new File(reposDir, moduleName)
		reposDir.mkdirs()
		// Make sure we delete symlinks and directories alike
		targetDirectory.delete() || targetDirectory.deleteDir()

		log.info "Cloning ${repository} into ${moduleName}"
		def commandLine = ["git", "clone", repository, targetDirectory]
		if (mirror) {
			commandLine.add "--mirror"
		}
		executeIn(null, commandLine)
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
