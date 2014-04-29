package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.PrideInitializer
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
		PrideInitializer.initializePride(prideDirectory, overwrite)
	}
}
