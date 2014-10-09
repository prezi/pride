package com.prezi.gradle.pride.cli;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.prezi.gradle.pride.Named;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.internal.LoggedNamedProgressAction;
import com.prezi.gradle.pride.internal.ProgressUtils;
import com.prezi.gradle.pride.vcs.RepoCache;
import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsManager;
import com.prezi.gradle.pride.vcs.VcsSupport;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.prezi.gradle.pride.cli.Configurations.PRIDE_HOME;
import static com.prezi.gradle.pride.cli.Configurations.REPO_BASE_URL;
import static com.prezi.gradle.pride.cli.Configurations.REPO_BRANCH;
import static com.prezi.gradle.pride.cli.Configurations.REPO_CACHE_ALWAYS;
import static com.prezi.gradle.pride.cli.Configurations.REPO_RECURSIVE;
import static com.prezi.gradle.pride.cli.Configurations.REPO_TYPE_DEFAULT;

public class ModuleAdder {
	private static final Logger logger = LoggerFactory.getLogger(ModuleAdder.class);

	public static List<String> addModules(Pride pride, Collection<ModuleToAdd> modules, VcsManager vcsManager) throws ConfigurationException, IOException {
		final RuntimeConfiguration config = pride.getConfiguration();
		boolean alwaysUseRepoCache = config.getBoolean(REPO_CACHE_ALWAYS);
		final String repoBaseUrl = config.getString(REPO_BASE_URL);
		final boolean recursive = config.getBoolean(REPO_RECURSIVE);
		final String defaultBranch = config.getString(REPO_BRANCH);
		String repoType = config.getString(REPO_TYPE_DEFAULT);

		// Get some support for our VCS
		final Vcs vcs = vcsManager.getVcs(repoType, config);
		final VcsSupport vcsSupport = vcs.getSupport();

		// Determine if we can use a repo cache
		final boolean useRepoCache;
		if (alwaysUseRepoCache && !vcsSupport.isMirroringSupported()) {
			logger.warn("Trying to use cache with a repository type that does not support local repository mirrors. Caching will be disabled.");
			useRepoCache = false;
		} else {
			useRepoCache = alwaysUseRepoCache;
		}

		final List<String> failedModules = Lists.newArrayList();
		ProgressUtils.execute(pride, modules, new LoggedNamedProgressAction<ModuleToAdd>("Adding") {
			private RepoCache repoCache = null;

			@Override
			public void execute(Pride pride, ModuleToAdd moduleEntry) throws IOException {
				String module = moduleEntry.getModule();
				String branch = moduleEntry.getBranch();
				if (Strings.isNullOrEmpty(branch)) {
					branch = defaultBranch;
				}

				try {
					String moduleName = vcsSupport.resolveRepositoryName(module);
					String repoUrl;
					if (!StringUtils.isEmpty(moduleName)) {
						repoUrl = module;
					} else {
						moduleName = module;
						repoUrl = getRepoUrl(repoBaseUrl, moduleName);
					}

					File moduleInPride = new File(pride.getRootDirectory(), moduleName);
					if (useRepoCache) {
						if (repoCache == null) {
							File cachePath = new File(config.getString(PRIDE_HOME) + "/cache");
							repoCache = new RepoCache(cachePath);
						}
						repoCache.checkoutThroughCache(vcsSupport, repoUrl, moduleInPride, branch, recursive);
					} else {
						vcsSupport.checkout(repoUrl, moduleInPride, branch, recursive, false);
					}
					pride.addModule(moduleName, repoUrl, branch, vcs);
				} catch (Exception ex) {
					logger.warn("Could not add module {}", module);
					logger.debug("Exception while adding module {}", module, ex);
					failedModules.add(module);
				}
			}
		});

		return failedModules;
	}

	private static String getRepoUrl(String repoBaseUrl, String moduleName) {
		if (repoBaseUrl == null) {
			throw new PrideException("You have specified a module name, but base URL for repositories is not set. " +
					"Either use a full repository URL, specify the base URL via --repo-base-url, " +
					"or set it in the global configuration (~/.prideconfig) as \"" + REPO_BASE_URL + "\". " +
					"See \'pride help config\' for more information.");
		}

		if (!repoBaseUrl.endsWith("/")) {
			repoBaseUrl += "/";
		}

		return repoBaseUrl + moduleName;
	}

	public static class ModuleToAdd implements Named {
		private final String module;
		private final String branch;

		public ModuleToAdd(String module, String branch) {
			this.module = module;
			this.branch = branch;
		}

		@Override
		public String getName() {
			return module;
		}

		public String getModule() {
			return module;
		}

		public String getBranch() {
			return branch;
		}
	}
}
