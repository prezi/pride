package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 10/04/14.
 */
@Command(name = "do", description = "Execute a command in all modules, or a subset of the modules in a pride")
class DoInPrideCommand extends PrideCommand {
	@Option(name = ["-I", "--include"],
			title = "repo",
			description = "Execute the command on repo (can be specified multiple times)")
	private List<File> inlcudeRepos

	@Option(name = "--exclude",
			title = "repo",
			description = "Do not execute command on repo (can be specified multiple times)")
	private List<File> excludeRepos

	@Arguments(required = true, description = "The command to execute")
	private List<String> commandLine

	@Override
	void run() {
		Pride pride = new Pride(prideDirectory)
		def modules = (inlcudeRepos ? inlcudeRepos : pride.modules).sort { it.name }.findAll { includeRepo ->
			return null == excludeRepos.find { excludeRepo ->
				includeRepo.absoluteFile.equals(excludeRepo.absoluteFile)
			}
		}

		modules.each { moduleDirectory ->
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
