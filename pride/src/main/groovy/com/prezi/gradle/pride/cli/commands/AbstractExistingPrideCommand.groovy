package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Pride

/**
 * Created by lptr on 15/04/14.
 */
abstract class AbstractExistingPrideCommand extends AbstractPrideCommand {
	@Override
	final void run() {
		def pride = Pride.getPride(prideDirectory)
		runInPride(pride)
	}

	abstract void runInPride(Pride pride)
}
