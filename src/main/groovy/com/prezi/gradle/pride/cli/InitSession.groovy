package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.SessionInitializer
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "init", description = "Initialize session")
class InitSession extends SessionCommand {

	@Option(name = ["-o", "--overwrite"],
			description = "Overwrite any existing sessions in the directory")
	private boolean overwrite

	@Override
	public void run() {
		SessionInitializer.initializeSession(sessionDirectory, overwrite)
	}
}
