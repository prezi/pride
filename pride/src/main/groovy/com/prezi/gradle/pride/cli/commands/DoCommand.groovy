package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.ProcessUtils
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 10/04/14.
 */
@Command(name = "do", description = "Execute a command on a set of the modules")
class DoCommand extends AbstractExistingPrideCommand {
	@Option(name = ["-I", "--include"],
			title = "repo",
			description = "Execute the command on repo (can be specified multiple times)")
	private List<File> includeRepos

	@Option(name = "--exclude",
			title = "repo",
			description = "Do not execute command on repo (can be specified multiple times)")
	private List<File> excludeRepos

	@Option(name = ["-b", "--bare"],
			description = "Only print the result of the executed commands")
	private boolean explicitBare

	@Arguments(required = true, description = "The command to execute")
	private List<String> commandLine

	@Override
	void runInPride(Pride pride) {
		def include = includeRepos?.sort() ?: pride.modules.collect { pride.getModuleDirectory(it.name) }
		def moduleDirectories = include.findAll { File includeRepo ->
			return null == excludeRepos.find { excludeRepo ->
				includeRepo.absoluteFile.equals(excludeRepo.absoluteFile)
			}
		}

		moduleDirectories.each { moduleDirectory ->
			if (!explicitBare) {
				log.info "\n${moduleDirectory} \$ ${commandLine.join(" ")}"
			}
			ProcessUtils.executeIn(moduleDirectory, commandLine)
		}
	}
}
