package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Pride;

import java.io.IOException;

public abstract class AbstractExistingPrideCommand extends AbstractPrideCommand {
	@Override
	protected final void runInternal() throws IOException {
		Pride pride = Pride.getPride(getPrideDirectory(), getConfiguration(), getVcsManager());
		runInPride(pride);
	}

	public abstract void runInPride(Pride pride) throws IOException;
}
