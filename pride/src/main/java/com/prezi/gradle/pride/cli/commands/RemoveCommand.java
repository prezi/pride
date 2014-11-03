package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import com.prezi.gradle.pride.internal.LoggedNamedProgressAction;
import com.prezi.gradle.pride.internal.ProgressUtils;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Command(name = "remove", description = "Remove modules from a pride")
public class RemoveCommand extends AbstractFilteredPrideCommand {

	@Option(name = {"-f", "--force"},
			description = "Remove modules even if there are local changes")
	private boolean force;

	@Arguments(description = "Modules to remove from the pride")
	private List<String> includeModules;

	@Override
	protected void executeInModules(final Pride pride, Collection<Module> modules) throws Exception {
		// Check if anything exists already
		if (!force) {
			Collection<Module> changedModules = Collections2.filter(modules, new Predicate<Module>() {
				@Override
				public boolean apply(Module module) {
					File moduleDir = pride.getModuleDirectory(module.getName());
					try {
						return module.getVcs().getSupport().hasChanges(moduleDir);
					} catch (Exception e) {
						throw Throwables.propagate(e);
					}
				}
			});

			if (!changedModules.isEmpty()) {
				throw new PrideException("These modules have changes: " + Joiner.on(", ").join(changedModules));
			}
		}

		// Remove modules
		ProgressUtils.execute(pride, modules, new LoggedNamedProgressAction<Module>("Removing") {
			@Override
			protected void execute(Pride pride, Module module) throws IOException {
				pride.removeModule(module.getName());
			}
		});
		pride.save();

		// Re-initialize pride
		new PrideInitializer(new GradleConnectorManager(pride.getConfiguration()), isVerbose()).reinitialize(pride);
	}

	@Override
	protected Collection<String> getIncludeModules() {
		return includeModules;
	}

	@Override
	protected void handleNoFilterSpecified() {
		throw new PrideException("No modules to remove have been specified");
	}

	@Override
	protected void handleNoMatchingModulesFound() {
		logger.warn("No matching modules found");
	}
}
