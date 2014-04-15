package com.prezi.gradle.pride.cli

import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
abstract class AbstractPrideCommand extends AbstractCommand {
	@Option(name = ["-p", "--pride-directory"], title = "directory",
			description = "Initializes the pride in the given directory instead of the current directory")
	private File explicitPrideDirectory

	protected File getPrideDirectory() {
		explicitPrideDirectory ?: new File(System.getProperty("user.dir"))
	}
}
