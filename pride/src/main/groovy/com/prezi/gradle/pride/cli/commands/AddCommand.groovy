package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.cli.CliConfiguration
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option
import org.apache.commons.configuration.Configuration

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "add", description = "Add modules to a pride")
class AddCommand extends AbstractExistingPrideCommand {
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
		def useRepoCache = configuration.getBoolean(CliConfiguration.REPO_CACHE_ALWAYS)
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
				repoCache.checkoutThroughCache(vcsSupport, repoUrl, moduleInPride)
			} else {
				vcsSupport.checkout(repoUrl, moduleInPride, false)
			}
			pride.addModule(moduleName, vcs)
		}
		try {
			pride.reinitialize()
		} catch (Exception ex) {
			throw new PrideException("There was a problem reinitializing the pride. Fix the errors above, and try again with\n\n\tpride init --force", ex)
		}
		pride.save()
	}

	@Override
	protected void overrideConfiguration(Configuration configuration) {
		super.overrideConfiguration(configuration)
		configuration.setProperty(CliConfiguration.REPO_BASE_URL, explicitRepoBaseUrl)
		configuration.setProperty(CliConfiguration.REPO_TYPE_DEFAULT, explicitRepoType)
		if (explicitUseRepoCache) {
			configuration.setProperty(CliConfiguration.REPO_CACHE_ALWAYS, true)
		}
		if (explicitDontUseRepoCache) {
			configuration.setProperty(CliConfiguration.REPO_CACHE_ALWAYS, false)
		}
	}
}
