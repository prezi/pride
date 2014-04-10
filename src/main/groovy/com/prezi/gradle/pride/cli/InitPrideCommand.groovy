package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideInitializer
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "init", description = "Initialize pride")
class InitPrideCommand extends PrideCommand {

	@Option(name = ["-o", "--overwrite"],
			description = "Overwrite any existing pride in the directory")
	private boolean overwrite

	@Override
	public void run() {
		PrideInitializer.initializePride(prideDirectory, overwrite)
	}
}
