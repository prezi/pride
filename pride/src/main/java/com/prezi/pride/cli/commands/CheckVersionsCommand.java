package com.prezi.pride.cli.commands;

import com.prezi.pride.Pride;
import io.airlift.command.Command;

@Command(name = "check-versions", description = "Check if dependency and project versions match", hidden = true)
public class CheckVersionsCommand extends AbstractPrideCommand {

	@Override
	public void executeInPride(Pride pride) throws Exception {
		logger.warn("Use ./gradlew checkDependencyVersions instead");
	}
}
