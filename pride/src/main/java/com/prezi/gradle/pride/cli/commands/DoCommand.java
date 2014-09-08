package com.prezi.gradle.pride.cli.commands;

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
@Command(name = "do", description = "Execute a command on a set of modules")
public class DoCommand extends AbstractPrideCommand {

	@Option(name = {"-I", "--include"},
			title = "module",
			description = "Execute the command on module (can be specified multiple times)")
	private List<String> includeModules;

	@Option(name = "--exclude",
			title = "module",
			description = "Do not execute command on module (can be specified multiple times)")
	private List<String> excludeModules;

	@Option(name = {"-b", "--bare"},
			description = "Only print the result of the executed commands")
	private boolean explicitBare;

	@Arguments(required = true,
			description = "The command to execute")
	private List<String> commandLine;

	@Override
	public void executeInPride(Pride pride) throws IOException {
		for (Module module : pride.filterModules(includeModules, excludeModules)) {
			File moduleDirectory = pride.getModuleDirectory(module.getName());
			if (!explicitBare) {
				logger.info("\n{} $ {}", moduleDirectory, StringUtils.join(commandLine, " "));
			}
			ProcessUtils.executeIn(moduleDirectory, commandLine);
		}
	}
}
