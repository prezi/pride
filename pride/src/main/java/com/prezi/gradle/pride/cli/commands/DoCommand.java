package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
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
import java.util.Collection;
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
		Collection<Module> includeModules;
		if (!this.includeModules.isEmpty()) {
			includeModules = Collections2.transform(this.includeModules, new Function<String, Module>() {
				@Override
				public Module apply(String module) {
					return pride.getModule(module);
				}
			});
		} else {
			includeModules = pride.getModules();
		}
		Collection<Module> modules = Collections2.filter(includeModules, new Predicate<Module>() {
			@Override
			public boolean apply(Module module) {
				return !excludeModules.contains(module.getName());
			}
		});
		for (Module module : modules) {
			File moduleDirectory = pride.getModuleDirectory(module.getName());
			if (!explicitBare) {
				logger.info("\n{} $ {}", moduleDirectory, StringUtils.join(commandLine, " "));
			}
			ProcessUtils.executeIn(moduleDirectory, commandLine);
		}
	}
}
