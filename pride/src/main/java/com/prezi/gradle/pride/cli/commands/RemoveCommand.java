package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.ProcessUtils;
import com.prezi.gradle.pride.cli.PrideInitializer;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Command(name = "remove", description = "Remove modules from a pride")
public class RemoveCommand extends AbstractExistingPrideCommand {

	@Option(name = {"-f", "--force"},
			description = "Remove modules even if there are local changes")
	private boolean force;

	@Arguments(required = true,
			description = "Modules to remove from the pride")
	private List<String> modulesNames;

	@Override
	public void runInPride(final Pride pride) throws IOException {
		// Check if anything exists already
		if (!force) {
			Collection<String> missingModules = Collections2.filter(modulesNames, new Predicate<String>() {
				@Override
				public boolean apply(String it) {
					return !pride.hasModule(it);
				}
			});
			if (!missingModules.isEmpty()) {
				throw new PrideException("These modules are missing: " + StringUtils.join(missingModules, ", "));
			}

			Collection<String> changedModules = Collections2.filter(modulesNames, new Predicate<String>() {
				@Override
				public boolean apply(String moduleName) {
					File moduleDir = pride.getModuleDirectory(moduleName);
					try {
						Process process = ProcessUtils.executeIn(moduleDir, new ArrayList<String>(Arrays.asList("git", "status", "--porcelain")), false);
						return !IOUtils.toString(process.getInputStream()).trim().isEmpty();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});

			if (!changedModules.isEmpty()) {
				throw new PrideException("These modules have uncommitted changes: " + StringUtils.join(changedModules, ", "));
			}
		}

		// Remove modules
		for (String moduleName : modulesNames) {
			pride.removeModule(moduleName);
		}
		pride.save();

		// Re-initialize pride
		PrideInitializer.reinitialize(pride);
	}
}

