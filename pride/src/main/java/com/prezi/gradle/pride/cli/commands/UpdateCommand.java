package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Strings;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.commands.actions.RefreshDependenciesAction;
import com.prezi.gradle.pride.internal.LoggedNamedProgressAction;
import com.prezi.gradle.pride.internal.ProgressUtils;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.prezi.gradle.pride.cli.Configurations.COMMAND_UPDATE_REFRESH_DEPENDENCIES;
import static com.prezi.gradle.pride.cli.Configurations.REPO_RECURSIVE;

@Command(name = "update", description = "Update modules a pride")
public class UpdateCommand extends AbstractFilteredPrideCommand {

	@Option(name = {"-D", "--refresh-dependencies"},
			description = "Refresh Gradle dependencies after update completed")
	private Boolean explicitRefreshDependencies;

	@Option(name = {"-r", "--recursive"},
			description = "Update sub-modules recursively")
	private Boolean explicitRecursive;

	@Option(name = {"--switch"},
			title = "branch",
			description = "Switch to branch")
	private String explicitSwitchToBranch;

	@Arguments(required = false,
			title = "modules",
			description = "The modules to update (updates all modules if none specified)")
	private List<String> includeModules;

	@Override
	protected void executeInModules(Pride pride, Collection<Module> modules) throws Exception {
		RuntimeConfiguration config = pride.getConfiguration();
		boolean refreshDependencies = config.override(COMMAND_UPDATE_REFRESH_DEPENDENCIES, explicitRefreshDependencies);
		final boolean recursive = config.override(REPO_RECURSIVE, explicitRecursive);

		ProgressUtils.execute(pride, modules, new LoggedNamedProgressAction<Module>("Updating") {
			@Override
			protected void execute(Pride pride, Module module) throws IOException {
				File moduleDir = pride.getModuleDirectory(module.getName());
				String moduleBranch = explicitSwitchToBranch;
				if (Strings.isNullOrEmpty(moduleBranch)) {
					moduleBranch = module.getVcs().getSupport().getBranch(pride.getModuleDirectory(module.getName()));
				}
				module.getVcs().getSupport().update(moduleDir, moduleBranch, recursive, false);
			}

			@Override
			public void execute(Pride pride, Module item, int index, int count) throws IOException {
				super.execute(pride, item, index, count);
				if (index < count - 1) {
					logger.info("");
				}
			}
		});
		if (refreshDependencies) {
			new RefreshDependenciesAction().refreshDependencies(pride);
		}
	}

	@Override
	protected Collection<String> getIncludeModules() {
		return includeModules;
	}
}
