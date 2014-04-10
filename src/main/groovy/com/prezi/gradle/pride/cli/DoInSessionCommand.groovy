package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import io.airlift.command.Arguments
import io.airlift.command.Command

/**
 * Created by lptr on 10/04/14.
 */
@Command(name = "do", description = "Execute a command in all modules in a session")
class DoInSessionCommand extends SessionCommand {
	@Arguments(required = true, description = "The command to execute")
	private List<String> commandLine

	@Override
	void run() {
		Pride pride = new Pride(sessionDirectory)
		pride.modules.each { moduleDirectory ->
			System.out.println("\n${moduleDirectory} \$ ${commandLine.join(" ")}")
			def process = commandLine.execute((String[]) null, moduleDirectory)
			process.waitForProcessOutput((OutputStream) System.out, System.err)

			def result = process.exitValue()
			if (result) {
				throw new PrideException("Failed to execute \"${commandLine}\" in \"${moduleDirectory}\", exit code: ${result}")
			}
		}
	}
}
