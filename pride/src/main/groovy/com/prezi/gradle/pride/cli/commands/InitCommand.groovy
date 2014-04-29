package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "init", description = "Initialize pride")
class InitCommand extends AbstractPrideCommand {

	@Option(name = ["-f", "--force"],
			description = "Force initialization of a pride, even if one already exists")
	private boolean overwrite

	@Override
	public void run() {
		if (!overwrite && Pride.containsPride(prideDirectory)) {
			throw new PrideException("A pride already exists in ${prideDirectory}")
		}
		Pride.create(prideDirectory, vcsManager)
	}
}
