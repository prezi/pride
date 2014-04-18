package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.PrideInitializer
import com.prezi.gradle.pride.internal.GitUtils
import com.prezi.gradle.pride.internal.RepoCache
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

	@Arguments(required = true, description = "Modules to add to the pride -- either module names to be resolved against the base URL, or full repository URLs")
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
		modules.each { module ->
			def (moduleName, repoUrl) = resolve(module)
			log.info "Adding ${moduleName} from ${repoUrl}"

			def moduleInPride = new File(prideDirectory, moduleName)
			if (useRepoCache) {
				def cache = new RepoCache(repoCachePath)
				cache.cloneRepository(repoUrl, moduleInPride)
			} else {
				GitUtils.cloneRepository(repoUrl, moduleInPride)
			}
		}

		// Re-initialize pride
		PrideInitializer.initializePride(pride.rootDirectory, true)
	}

	private String[] resolve(String module) {
		def m = module =~ /^(?:git@|(?:https?):\\/+).*[:\\/]([-\._\w]+?)(?:\.git)?\\/?$/
		if (m) {
			return [m[0][1], module]
		} else {
			return [module, repoBaseUrl + module]
		}
	}

	private String getRepoBaseUrl() {
		String repoBaseUrl = explicitRepoBaseUrl ?: configuration.repoBaseUrl
		if (repoBaseUrl == null) {
			throw new PrideException("You have specified a module name, but base URL for Git repos is not set. " +
					"Either use a full repository URL, specify the base URL via --repo-base-url, or set it in the global configuration -- see pride help config.")
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
