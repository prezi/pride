package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.commands.actions.InitAction;
import com.prezi.gradle.pride.cli.commands.actions.InitActionBase;
import com.prezi.gradle.pride.cli.commands.actions.InitActionFromImportedConfig;
import io.airlift.command.Command;
import io.airlift.command.Option;

import static com.prezi.gradle.pride.cli.Configurations.GRADLE_WRAPPER;

@Command(name = "init", description = "Initialize pride")
public class InitCommand extends AbstractConfiguredCommand {

	@Option(name = {"-f", "--force"},
			description = "Force initialization of a pride, even if one already exists")
	private boolean explicitForce;

	@Option(name = "--with-wrapper",
			description = "Add a Gradle wrapper")
	private boolean explicitWithWrapper;

	@Option(name = "--no-wrapper",
			description = "Do not add Gradle wrapper")
	private boolean explicitNoWrapper;

	@Option(name = "--no-add-existing",
			description = "Do not add existing modules in the pride directory to the pride")
	private boolean explicitNoAddExisting;

	@Option(name = "--ignore-config",
			description = "Ignore existing pride's configuration (to be used with --force)")
	private boolean explicitIgnoreConfig;

	@Option(name = "--from-config",
			title = "file or URL",
			description = "Load configuration and modules from existing configuration")
	private String explicitFromConfig;

	@Override
	protected int executeWithConfiguration(RuntimeConfiguration globalConfig) throws Exception {
		boolean addWrapper = globalConfig.override(GRADLE_WRAPPER, explicitWithWrapper, explicitNoWrapper);

		InitActionBase initAction;
		if (explicitFromConfig == null) {
			initAction = InitAction.create(getPrideDirectory(), globalConfig, getVcsManager(), explicitForce, !explicitNoAddExisting, explicitIgnoreConfig);
		} else {
			initAction = InitActionFromImportedConfig.create(getPrideDirectory(), globalConfig, getVcsManager(), explicitFromConfig);
		}
		return initAction.createPride(addWrapper, isVerbose());
	}
}
