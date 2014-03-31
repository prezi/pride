package com.prezi.gradle.pride.cli

import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
abstract class SessionCommand implements Runnable {
	@Option(name = ["-s", "--session-directory"], title = "directory",
			description = "Initializes the session in the given directory instead of the current directory")
	private File explicitSessionDirectory

	protected File getSessionDirectory() {
		explicitSessionDirectory ?: new File(System.getProperty("user.dir"))
	}
}
