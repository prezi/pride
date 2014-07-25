package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Pride;

public abstract class AbstractExistingPrideCommand extends AbstractPrideCommand {

	@Override
	public Integer call() throws Exception {
		Pride pride = Pride.getPride(getPrideDirectory(), getConfiguration(), getVcsManager());
		runInPride(pride);
		return 0;
	}

	public abstract void runInPride(Pride pride) throws Exception;
}
