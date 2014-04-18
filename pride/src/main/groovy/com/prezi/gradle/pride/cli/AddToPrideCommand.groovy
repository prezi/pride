package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.PrideInitializer
import com.prezi.gradle.pride.internal.GitUtils
import com.prezi.gradle.pride.internal.ProcessUtils
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "add", description = "Add modules to a pride")
class AddToPrideCommand extends AbstractExistingPrideCommand {
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
	void runInPride(Pride pride) {
		def useRepoCache = explicitUseRepoCache || (!explicitDontUseRepoCache && configuration.repoCacheAlways)

		// Check if anything exists already
		if (!overwrite) {
			def existingRepos = modules.findAll { new File(pride.rootDirectory, it).exists() }
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
					ProcessUtils.executeIn(moduleInCache, ["git", "fetch", "--all"])
				}
				def moduleInPride = cloneModuleFromCache(moduleName, pride.rootDirectory)
				ProcessUtils.executeIn(moduleInPride, ["git", "remote", "set-url", "origin", getModuleRepoUrl(moduleName)])
			} else {
				cloneModuleFromExternal(moduleName, pride.rootDirectory, false)
			}
		}

		// Re-initialize pride
		PrideInitializer.initializePride(pride.rootDirectory, true)
	}

	private File cloneModuleFromCache(String moduleName, File reposDir) {
		def repository = new File(repoCachePath, moduleName).absolutePath
		GitUtils.cloneRepository(repository, new File(reposDir, moduleName), false)
	}

	private File cloneModuleFromExternal(String moduleName, File reposDir, boolean mirror) {
		def repository = getModuleRepoUrl(moduleName)
		GitUtils.cloneRepository(repository, new File(reposDir, moduleName), mirror)
	}

	private String getModuleRepoUrl(String moduleName) {
		return repoBaseUrl + moduleName + ".git"
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
