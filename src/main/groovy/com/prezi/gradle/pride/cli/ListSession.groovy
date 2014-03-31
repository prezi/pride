package com.prezi.gradle.pride.cli

import io.airlift.command.Command

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "list", description = "List projects in a session")
class ListSession extends SessionCommand {

	@Override
	void run() {
		System.out.println("This would list the projects loaded in a session")
	}
}
