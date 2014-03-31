package com.prezi.gradle.pride.cli

import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "add", description = "Add modules to a session")
class AddToSession extends SessionCommand {

	@Option(name = ["-o", "--overwrite"],
			description = "Overwrite any existing sessions in the directory")
	private boolean overwrite

	@Arguments(required = true, description = "Repositories to add")
	private List<String> repositories

	@Override
	void run() {
		if (!overwrite) {
			def existingRepos = repositories.findAll { new File(sessionDirectory, it).exists() }
			if (existingRepos) {
				throw new IllegalArgumentException("These repositories already exist: ${existingRepos.join(", ")}")
			}
		}

		
	}
}
