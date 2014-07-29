package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.ModuleAdder;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Collections2.filter;
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
		config.override(REPO_BASE_URL, explicitRepoBaseUrl);
		config.override(REPO_TYPE_DEFAULT, explicitRepoType);
		config.override(REPO_CACHE_ALWAYS, explicitUseRepoCache, explicitNoRepoCache);
		config.override(REPO_RECURSIVE, explicitRecursive);

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

		// Clone repositories
		List<String> failedModules = ModuleAdder.addModules(pride, modules, getVcsManager());

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
}
