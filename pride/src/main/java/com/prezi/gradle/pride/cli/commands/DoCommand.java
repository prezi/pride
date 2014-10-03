package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Named;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.ProcessUtils;
import com.prezi.gradle.pride.internal.LoggedProgressAction;
import com.prezi.gradle.pride.internal.ProgressAction;
import com.prezi.gradle.pride.internal.ProgressUtils;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.File;
import java.io.IOException;
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
	protected void executeInModules(final Pride pride, Collection<Module> modules) throws Exception {
		Collection<File> moduleDirs = Collections2.transform(modules, new Function<Module, File>() {
			@Override
			public File apply(Module module) {
				return pride.getModuleDirectory(module.getName());
			}
		});
		ProgressAction<File> action;
		if (!explicitBare) {
			action = new LoggedProgressAction<File>("", "$ " + Joiner.on(" ").join(commandLine), Named.TOSTRING_NAMER) {
				@Override
				protected void execute(Pride pride, File directory) throws IOException {
					executeInDirectory(directory);
				}

				@Override
				public void execute(Pride pride, File directory, int index, int count) throws IOException {
					super.execute(pride, directory, index, count);
					if (index < count - 1) {
						logger.info("");
					}
				}
			};
		} else {
			action = new ProgressAction<File>() {
				@Override
				public void execute(Pride pride, File directory, int index, int count) throws IOException {
					executeInDirectory(directory);
				}
			};
		}

		ProgressUtils.execute(pride, moduleDirs, action);
	}

	private void executeInDirectory(File directory) throws IOException {
		ProcessUtils.executeIn(directory, commandLine);
	}

	@Override
	protected Collection<String> getIncludeModules() {
		return includeModules;
	}
}
