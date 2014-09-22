package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.ProcessUtils;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;

@Command(name = "do", description = "Execute a command on a set of modules")
public class DoCommand extends AbstractFilteredPrideCommand {

	@Option(name = {"-I", "--include"},
			title = "regex",
			description = "Execute the command on module (can be specified multiple times)")
	private List<String> includeModules;

	@Option(name = {"-B", "--bare"},
			description = "Only print the result of the executed commands")
	private boolean explicitBare;

	@Arguments(required = true,
			description = "The command to execute")
	private List<String> commandLine;

	@Override
	protected void executeInModules(Pride pride, Collection<Module> modules) throws Exception {
		for (Module module : modules) {
			File moduleDirectory = pride.getModuleDirectory(module.getName());
			if (!explicitBare) {
				logger.info("\n{} $ {}", moduleDirectory, StringUtils.join(commandLine, " "));
			}
			ProcessUtils.executeIn(moduleDirectory, commandLine);
		}
	}

	@Override
	protected Collection<String> getIncludeModules() {
		return includeModules;
	}
}
