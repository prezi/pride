package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
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
import java.util.TreeSet;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
@Command(name = "do", description = "Execute a command on a set of the modules")
public class DoCommand extends AbstractExistingPrideCommand {

	@Option(name = {"-I", "--include"},
			title = "repo",
			description = "Execute the command on repo (can be specified multiple times)")
	private List<File> includeRepos;

	@Option(name = "--exclude",
			title = "repo",
			description = "Do not execute command on repo (can be specified multiple times)")
	private List<File> excludeRepos;

	@Option(name = {"-b", "--bare"},
			description = "Only print the result of the executed commands")
	private boolean explicitBare;

	@Arguments(required = true,
			description = "The command to execute")
	private List<String> commandLine;

	@Override
	public void runInPride(final Pride pride) throws IOException {
		Collection<File> includeDirectories;
		if (!includeRepos.isEmpty()) {
			includeDirectories = new TreeSet<File>(includeRepos);
		} else {
			includeDirectories = Collections2.transform(pride.getModules(), new Function<Module, File>() {
				@Override
				public File apply(Module module) {
					return pride.getModuleDirectory(module.name);
				}
			});
		}

		Collection<File> moduleDirectories = Collections2.filter(includeDirectories, new Predicate<File>() {
			@Override
			public boolean apply(final File includeRepo) {
				return null == Iterables.find(excludeRepos, new Predicate<File>() {
					@Override
					public boolean apply(final File excludeRepo) {
						return includeRepo.getAbsoluteFile().equals(excludeRepo.getAbsoluteFile());
					}
				}, null);
			}
		});

		for (File moduleDirectory : moduleDirectories) {
			if (!explicitBare) {
				logger.info("\n{} $ {}", moduleDirectory, StringUtils.join(commandLine, " "));
			}
			ProcessUtils.executeIn(moduleDirectory, commandLine);
		}
	}
}
