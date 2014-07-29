package com.prezi.gradle.pride.cli;

import com.google.common.collect.Lists;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.vcs.RepoCache;
import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsManager;
import com.prezi.gradle.pride.vcs.VcsSupport;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static com.prezi.gradle.pride.cli.Configurations.PRIDE_HOME;
import static com.prezi.gradle.pride.cli.Configurations.REPO_BASE_URL;
import static com.prezi.gradle.pride.cli.Configurations.REPO_CACHE_ALWAYS;
import static com.prezi.gradle.pride.cli.Configurations.REPO_RECURSIVE;
import static com.prezi.gradle.pride.cli.Configurations.REPO_TYPE_DEFAULT;

public class ModuleAdder {
	private static final Logger logger = LoggerFactory.getLogger(ModuleAdder.class);

	public static List<String> addModules(Pride pride, Collection<String> modules, VcsManager vcsManager) throws ConfigurationException {
		RuntimeConfiguration config = pride.getConfiguration();
		boolean useRepoCache = config.getBoolean(REPO_CACHE_ALWAYS);
		String repoBaseUrl = config.getString(REPO_BASE_URL);
		boolean recursive = config.getBoolean(REPO_RECURSIVE);
		String repoType = config.getString(REPO_TYPE_DEFAULT);

		// Get some support for our VCS
		Vcs vcs = vcsManager.getVcs(repoType, config);
		VcsSupport vcsSupport = vcs.getSupport();

		// Determine if we can use a repo cache
		if (useRepoCache && !vcsSupport.isMirroringSupported()) {
			logger.warn("Trying to use cache with a repository type that does not support local repository mirrors. Caching will be disabled.");
			useRepoCache = false;
		}

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

				logger.info("Adding {} from {}", moduleName, repoUrl);

				File moduleInPride = new File(pride.getRootDirectory(), moduleName);
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
}
