package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.cli.Configuration
import com.prezi.gradle.pride.vcs.RepoCache
import com.prezi.gradle.pride.vcs.Vcs
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "add", description = "Add modules to a pride")
class AddCommand extends AbstractExistingPrideCommand {
	private static final Logger log = LoggerFactory.getLogger(AddCommand)

	@Option(name = ["-o", "--overwrite"],
			description = "Overwrite existing modules in the pride")
	private boolean overwrite

	@Option(name = ["-B", "--repo-base-url"],
			title = "url",
			description = "Base URL for module repositories")
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

	@Option(name = ["-T", "--repo-type"],
			title = "type",
			description = "Repository type")
	private String explicitRepoType

	@Arguments(required = true,
			description = "Modules to add to the pride -- either module names to be resolved against the base URL, or full repository URLs")
	private List<String> modules

	@Override
	void runInPride(Pride pride) {
		// Check if anything exists already
		if (!overwrite) {
			def existingModules = modules.findAll { pride.hasModule(it) }
			if (existingModules) {
				throw new PrideException("These modules already exist in pride: ${existingModules.join(", ")}")
			}
			def existingRepos = modules.findAll { new File(pride.rootDirectory, it).exists() }
			if (existingRepos) {
				throw new PrideException("These directories already exist: ${existingRepos.join(", ")}")
			}
		}

		// Get some support for our VCS
		def vcs = getVcs()
		def vcsSupport = vcs.support

		// Determine if we can use a repo cache
		def useRepoCache = explicitUseRepoCache || (!explicitDontUseRepoCache && configuration.repoCacheAlways)
		if (useRepoCache && !vcsSupport.mirroringSupported) {
			log.warn("Trying to use cache with a repository type that does not support local repository mirrors. Caching will be disabled.")
			useRepoCache = false
		}

		// Clone repositories
		modules.each { module ->
			def moduleName = vcsSupport.resolveRepositoryName(module)
			def repoUrl
			if (moduleName) {
				repoUrl = module
			} else {
				moduleName = module
				repoUrl = repoBaseUrl + moduleName
			}
			log.info "Adding ${moduleName} from ${repoUrl}"

			def moduleInPride = new File(prideDirectory, moduleName)
			if (useRepoCache) {
				def cache = new RepoCache(repoCachePath)
				cache.checkoutThroughCache(vcsSupport, repoUrl, moduleInPride)
			} else {
				vcsSupport.checkout(repoUrl, moduleInPride, false)
			}
			pride.addModule(moduleName, vcs)
		}
		pride.reinitialize()
		pride.save()
	}

	private String getRepoBaseUrl() {
		String repoBaseUrl = explicitRepoBaseUrl ?: configuration.repoBaseUrl
		if (repoBaseUrl == null) {
			throw invalidOptionException("You have specified a module name, but base URL for Git repos is not set",
					"a full repository URL, specify the base URL via --repo-base-url", Configuration.REPO_BASE_URL)
		}
		if (!repoBaseUrl.endsWith("/")) {
			repoBaseUrl += "/"
		}
		return repoBaseUrl
	}

	private File getRepoCachePath() {
		def path = explicitRepoCachePath ?: configuration.repoCachePath
		if (!path) {
			throw invalidOptionException("Repository cache path is not set", "--repo-cache-path", Configuration.REPO_CACHE_PATH)
		}
		return path
	}

	private Vcs getVcs() {
		def repoType = explicitRepoType ?: configuration.repoTypeDefault
		if (!repoType) {
			throw invalidOptionException("Repository type is not set", "--repo-type", Configuration.REPO_TYPE_DEFAULT)
		}
		return vcsManager.getVcs(repoType)
	}
}
