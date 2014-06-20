package com.prezi.gradle.pride.cli.commands;

import com.google.common.collect.Lists;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.ProcessUtils;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
@Command(name = "do", description = "Execute a command on a set of the modules")
public class DoCommand extends AbstractExistingPrideCommand {

	@Option(name = {"-I", "--include"},
			title = "repo",
			description = "Execute the command on module (can be specified multiple times)")
	private List<String> includeModules = Lists.newArrayList();

	@Option(name = "--exclude",
			title = "repo",
			description = "Do not execute command on module (can be specified multiple times)")
	private List<String> excludeModules = Lists.newArrayList();

	@Option(name = {"-b", "--bare"},
			description = "Only print the result of the executed commands")
	private boolean explicitBare;

	@Arguments(required = true,
			description = "The command to execute")
	private List<String> commandLine;

	@Override
	public void runInPride(final Pride pride) throws IOException {
		for (Module module : pride.filterModules(includeModules, excludeModules)) {
			File moduleDirectory = pride.getModuleDirectory(module.getName());
			if (!explicitBare) {
				logger.info("\n{} $ {}", moduleDirectory, StringUtils.join(commandLine, " "));
			}
			ProcessUtils.executeIn(moduleDirectory, commandLine);
		}
	}
}
