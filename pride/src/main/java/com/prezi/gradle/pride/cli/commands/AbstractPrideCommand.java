package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Pride;
import org.apache.commons.configuration.Configuration;

public abstract class AbstractPrideCommand extends AbstractConfiguredCommand {
	@Override
	final protected int executeWithConfiguration(Configuration globalConfig) throws Exception {
		Pride pride = Pride.getPride(getPrideDirectory(), globalConfig, getVcsManager());
		executeInPride(pride);
		return 0;
	}

	public abstract void executeInPride(Pride pride) throws Exception;
}
