package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.CliConfiguration;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsSupport;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Collections2.filter;

@Command(name = "add", description = "Add modules to a pride")
public class AddCommand extends AbstractExistingPrideCommand {

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
	private boolean explicitDontUseRepoCache;

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
	public void runInPride(final Pride pride) throws IOException {
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
		Vcs vcs = getDefaultVcs();
		VcsSupport vcsSupport = vcs.getSupport();

		// Determine if we can use a repo cache
		boolean useRepoCache = getConfiguration().getBoolean(CliConfiguration.REPO_CACHE_ALWAYS);
		if (useRepoCache && !vcsSupport.isMirroringSupported()) {
			logger.warn("Trying to use cache with a repository type that does not support local repository mirrors. Caching will be disabled.");
			useRepoCache = false;
		}
		boolean recursive = getConfiguration().getBoolean(CliConfiguration.REPO_RECURSIVE);

		// Clone repositories
		List<String> failedModules = Lists.newArrayList();
		for (String module : modules) {
			try {
				String moduleName = vcsSupport.resolveRepositoryName(module);
				String repoUrl;
				if (!StringUtils.isEmpty(moduleName)) {
					repoUrl = module;
				} else {
					moduleName = module;
					repoUrl = getRepoBaseUrl() + moduleName;
				}

				logger.info("Adding " + moduleName + " from " + repoUrl);

				File moduleInPride = new File(getPrideDirectory(), moduleName);
				if (useRepoCache) {
					getRepoCache().checkoutThroughCache(vcsSupport, repoUrl, moduleInPride, recursive);
				} else {
					vcsSupport.checkout(repoUrl, moduleInPride, recursive, false);
				}
				pride.addModule(moduleName, vcs);
			} catch (Exception ex) {
				logger.debug("Could not add {}", module, ex);
				failedModules.add(module);
			}
		}

		pride.save();

		try {
			PrideInitializer.reinitialize(pride);
		} catch (Exception ex) {
			throw new PrideException("There was a problem reinitializing the pride. Fix the errors above, and try again with\n\n\tpride init --force", ex);
		} finally {
			if (!failedModules.isEmpty()) {
				logger.error("Could not add the following modules:\n\n\t* {}", Joiner.on("\n\t* ").join(failedModules));
			}
		}
	}

	@Override
	protected void overrideConfiguration(Configuration configuration) {
		super.overrideConfiguration(configuration);
		if (!StringUtils.isEmpty(explicitRepoBaseUrl)) {
			configuration.setProperty(CliConfiguration.REPO_BASE_URL, explicitRepoBaseUrl);
		}

		if (!StringUtils.isEmpty(explicitRepoType)) {
			configuration.setProperty(CliConfiguration.REPO_TYPE_DEFAULT, explicitRepoType);
		}

		if (explicitUseRepoCache) {
			configuration.setProperty(CliConfiguration.REPO_CACHE_ALWAYS, true);
		}

		if (explicitDontUseRepoCache) {
			configuration.setProperty(CliConfiguration.REPO_CACHE_ALWAYS, false);
		}
		if (explicitRecursive != null) {
			configuration.setProperty(CliConfiguration.REPO_RECURSIVE, explicitRecursive);
		}
	}
}
