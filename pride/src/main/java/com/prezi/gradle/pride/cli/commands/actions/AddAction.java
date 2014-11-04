package com.prezi.gradle.pride.cli.commands.actions;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.ExportedModule;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import com.prezi.gradle.pride.internal.LoggedNamedProgressAction;
import com.prezi.gradle.pride.internal.ProgressUtils;
import com.prezi.gradle.pride.vcs.RepoCache;
import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsSupport;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Collections2.filter;
import static com.prezi.gradle.pride.cli.Configurations.PRIDE_HOME;
import static com.prezi.gradle.pride.cli.Configurations.REPO_BRANCH;
import static com.prezi.gradle.pride.cli.Configurations.REPO_CACHE_ALWAYS;
import static com.prezi.gradle.pride.cli.Configurations.REPO_RECURSIVE;

public class AddAction {
	private final Pride pride;
	private final boolean overwrite;
	private final boolean useRepoCache;
	private final boolean noRepoCache;
	private final Boolean recursive;
	private final boolean verbose;

	public AddAction(Pride pride, boolean overwrite, boolean useRepoCache, boolean noRepoCache, Boolean recursive, boolean verbose) {
		this.pride = pride;
		this.overwrite = overwrite;
		this.useRepoCache = useRepoCache;
		this.noRepoCache = noRepoCache;
		this.recursive = recursive;
		this.verbose = verbose;
	}

	public void addModules(Collection<ExportedModule> modules) throws Exception {
		final RuntimeConfiguration config = pride.getConfiguration();
		final boolean alwaysUseRepoCache = config.override(REPO_CACHE_ALWAYS, useRepoCache, noRepoCache);
		final boolean recursive = config.override(REPO_RECURSIVE, this.recursive);
		final String defaultRevision = config.getString(REPO_BRANCH);

		// Check if anything exists already
		if (!overwrite) {
			Collection<ExportedModule> existingModules = filter(modules, new Predicate<ExportedModule>() {
				@Override
				public boolean apply(ExportedModule it) {
					return pride.hasModule(it.getModule());
				}
			});
			if (!existingModules.isEmpty()) {
				throw new PrideException("These modules already exist in pride: " + StringUtils.join(existingModules, ", "));
			}
			Collection<ExportedModule> existingRepos = filter(modules, new Predicate<ExportedModule>() {
				@Override
				public boolean apply(ExportedModule it) {
					return new File(pride.getRootDirectory(), it.getModule()).exists();
				}
			});
			if (!existingRepos.isEmpty()) {
				throw new PrideException("These directories already exist: " + StringUtils.join(existingRepos, ", "));
			}
		}

		final List<String> failedModules = Lists.newArrayList();
		ProgressUtils.execute(pride, modules, new LoggedNamedProgressAction<ExportedModule>("Adding") {
			private RepoCache repoCache = null;

			@Override
			public void execute(Pride pride, ExportedModule moduleEntry) throws IOException {
				String moduleName = moduleEntry.getModule();
				String repoUrl = moduleEntry.getRemote();
				Vcs vcs = moduleEntry.getVcs();
				VcsSupport vcsSupport = vcs.getSupport();
				String revision = moduleEntry.getRevision();
				if (Strings.isNullOrEmpty(revision)) {
					revision = defaultRevision;
				}

				// Determine if we can use a repo cache
				final boolean useRepoCache;
				if (alwaysUseRepoCache && !vcsSupport.isMirroringSupported()) {
					logger.warn("Cannot use repo cache for {}.", moduleName);
					useRepoCache = false;
				} else {
					useRepoCache = alwaysUseRepoCache;
				}

				try {
					File moduleInPride = new File(pride.getRootDirectory(), moduleName);
					if (useRepoCache) {
						if (repoCache == null) {
							File cachePath = new File(config.getString(PRIDE_HOME) + "/cache");
							repoCache = new RepoCache(cachePath);
						}
						repoCache.checkoutThroughCache(vcsSupport, repoUrl, moduleInPride, revision, recursive);
					} else {
						vcsSupport.checkout(repoUrl, moduleInPride, revision, recursive, false);
					}
					pride.addModule(moduleName, vcs);
				} catch (Exception ex) {
					logger.warn("Could not add module {}", moduleName);
					logger.debug("Exception while adding module {}", moduleName, ex);
					failedModules.add(moduleName);
				}
			}
		});

		pride.save();
		new PrideInitializer(new GradleConnectorManager(config), verbose).reinitialize(pride);

		if (!failedModules.isEmpty()) {
			throw new PrideException("Could not add the following modules:\n\n\t* " + Joiner.on("\n\t* ").join(failedModules));
		}
	}
}
