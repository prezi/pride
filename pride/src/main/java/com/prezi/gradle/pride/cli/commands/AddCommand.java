package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import com.prezi.gradle.pride.vcs.RepoCache;
import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsSupport;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Collections2.filter;
import static com.prezi.gradle.pride.cli.Configurations.PRIDE_HOME;
import static com.prezi.gradle.pride.cli.Configurations.REPO_BASE_URL;
import static com.prezi.gradle.pride.cli.Configurations.REPO_CACHE_ALWAYS;
import static com.prezi.gradle.pride.cli.Configurations.REPO_RECURSIVE;
import static com.prezi.gradle.pride.cli.Configurations.REPO_TYPE_DEFAULT;

@Command(name = "add", description = "Add modules to a pride")
public class AddCommand extends AbstractPrideCommand {

	@Option(name = {"-o", "--overwrite"},
			description = "Overwrite existing modules in the pride")
	private boolean overwrite;

	@Option(name = {"-B", "--repo-base-url"},
			title = "url",
			description = "Base URL for module repositories")
	private String explicitRepoBaseUrl;

	@Option(name = {"-c", "--use-repo-cache"},
			description = "Use local repo cache")
	private boolean explicitUseRepoCache;

	@Option(name = {"--no-repo-cache"},
			description = "Do not use local repo cache")
	private boolean explicitNoRepoCache;

	@Option(name = {"-r", "--recursive"},
			description = "Update sub-modules recursively")
	private Boolean explicitRecursive;

	@Option(name = {"-T", "--repo-type"},
			title = "type",
			description = "Repository type")
	private String explicitRepoType;

	@Arguments(required = true,
			description = "Modules to add to the pride -- either module names to be resolved against the base URL, or full repository URLs")
	private List<String> modules;

	@Override
	public void executeInPride(final Pride pride) throws Exception {
		RuntimeConfiguration config = pride.getConfiguration();
		String repoBaseUrl = config.override(REPO_BASE_URL, explicitRepoBaseUrl);
		String repoType = config.override(REPO_TYPE_DEFAULT, explicitRepoType);
		boolean useRepoCache = config.override(REPO_CACHE_ALWAYS, explicitUseRepoCache, explicitNoRepoCache);
		boolean recursive = config.override(REPO_RECURSIVE, explicitRecursive);

		// Check if anything exists already
		if (!overwrite) {
			Collection<String> existingModules = filter(modules, new Predicate<String>() {
				@Override
				public boolean apply(String it) {
					return pride.hasModule(it);
				}
			});
			if (!existingModules.isEmpty()) {
				throw new PrideException("These modules already exist in pride: " + StringUtils.join(existingModules, ", "));
			}
			Collection<String> existingRepos = filter(modules, new Predicate<String>() {
				@Override
				public boolean apply(String it) {
					return new File(pride.getRootDirectory(), it).exists();
				}
			});
			if (!existingRepos.isEmpty()) {
				throw new PrideException("These directories already exist: " + StringUtils.join(existingRepos, ", "));
			}
		}

		// Get some support for our VCS
		Vcs vcs = getVcsManager().getVcs(repoType, config);
		VcsSupport vcsSupport = vcs.getSupport();

		// Determine if we can use a repo cache
		if (useRepoCache && !vcsSupport.isMirroringSupported()) {
			logger.warn("Trying to use cache with a repository type that does not support local repository mirrors. Caching will be disabled.");
			useRepoCache = false;
		}

		// Clone repositories
		List<String> failedModules = Lists.newArrayList();
		RepoCache repoCache = null;
		for (String module : modules) {
			try {
				String moduleName = vcsSupport.resolveRepositoryName(module);
				String repoUrl;
				if (!StringUtils.isEmpty(moduleName)) {
					repoUrl = module;
				} else {
					moduleName = module;
					repoUrl = getRepoUrl(repoBaseUrl, moduleName);
				}

				logger.info("Adding " + moduleName + " from " + repoUrl);

				File moduleInPride = new File(getPrideDirectory(), moduleName);
				if (useRepoCache) {
					if (repoCache == null) {
						File cachePath = new File(config.getString(PRIDE_HOME) + "/cache");
						repoCache = new RepoCache(cachePath);
					}
					repoCache.checkoutThroughCache(vcsSupport, repoUrl, moduleInPride, recursive);
				} else {
					vcsSupport.checkout(repoUrl, moduleInPride, recursive, false);
				}
				pride.addModule(moduleName, repoUrl, vcs);
			} catch (Exception ex) {
				logger.debug("Could not add {}", module, ex);
				failedModules.add(module);
			}
		}

		pride.save();

		try {
			new PrideInitializer(new GradleConnectorManager(config)).reinitialize(pride);
		} catch (Exception ex) {
			throw new PrideException("There was a problem reinitializing the pride. Fix the errors above, and try again with\n\n\tpride init --force", ex);
		} finally {
			if (!failedModules.isEmpty()) {
				logger.error("Could not add the following modules:\n\n\t* {}", Joiner.on("\n\t* ").join(failedModules));
			}
		}
	}

	protected static String getRepoUrl(String repoBaseUrl, String moduleName) {
		if (repoBaseUrl == null) {
			throw invalidOptionException("You have specified a module name, but base URL for Git repos is not set", "a full repository URL, specify the base URL via --repo-base-url", REPO_BASE_URL);
		}

		if (!repoBaseUrl.endsWith("/")) {
			repoBaseUrl += "/";
		}

		return repoBaseUrl + moduleName;
	}
}
