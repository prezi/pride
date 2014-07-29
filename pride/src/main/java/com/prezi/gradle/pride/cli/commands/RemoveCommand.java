package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;

@Command(name = "remove", description = "Remove modules from a pride")
public class RemoveCommand extends AbstractPrideCommand {

	@Option(name = {"-f", "--force"},
			description = "Remove modules even if there are local changes")
	private boolean force;

	@Arguments(required = true,
			description = "Modules to remove from the pride")
	private List<String> modulesNames;

	@Override
	public void executeInPride(final Pride pride) throws Exception {
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
					Module module = pride.getModule(moduleName);
					File moduleDir = pride.getModuleDirectory(module.getName());
					try {
						return module.getVcs().getSupport().hasChanges(moduleDir);
					} catch (Exception e) {
						throw Throwables.propagate(e);
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
		new PrideInitializer(new GradleConnectorManager(pride.getConfiguration()), isVerbose()).reinitialize(pride);
	}
}
