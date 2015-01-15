package com.prezi.pride.cli.commands;

import com.prezi.pride.Pride;
import com.prezi.pride.RuntimeConfiguration;
import com.prezi.pride.cli.commands.actions.InitAction;
import com.prezi.pride.cli.commands.actions.RefreshDependenciesAction;
import io.airlift.command.Command;
import io.airlift.command.Option;

import static com.prezi.pride.cli.Configurations.COMMAND_REINIT_REFRESH_DEPENDENCIES;
import static com.prezi.pride.cli.Configurations.GRADLE_WRAPPER;

@Command(name = "reinit", description = "Re-initialize the configuration of an existing pride")
public class ReinitCommand extends AbstractConfiguredCommand {

	@Option(name = "--with-wrapper",
			description = "Add a Gradle wrapper")
	private boolean explicitWithWrapper;

	@Option(name = "--no-wrapper",
			description = "Do not add Gradle wrapper")
	private boolean explicitNoWrapper;

	@Option(name = {"-D", "--refresh-dependencies"},
			description = "Refresh Gradle dependencies after update completed")
	private Boolean explicitRefreshDependencies;

	@Override
	protected void executeWithConfiguration(RuntimeConfiguration config) throws Exception {
		Pride pride = Pride.getPride(getPrideDirectory(), config, getVcsManager());
		boolean addWrapper = pride.getConfiguration().override(GRADLE_WRAPPER, explicitWithWrapper, explicitNoWrapper);
		InitAction.create(pride.getRootDirectory(), config, getVcsManager(), true, true, false).createPride(addWrapper, isVerbose());

		boolean refreshDependencies = config.override(COMMAND_REINIT_REFRESH_DEPENDENCIES, explicitRefreshDependencies);
		if (refreshDependencies) {
			new RefreshDependenciesAction().refreshDependencies(pride);
		}
	}
}
