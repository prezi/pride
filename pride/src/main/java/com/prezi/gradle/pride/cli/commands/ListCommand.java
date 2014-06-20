package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.File;
import java.io.FileFilter;

@Command(name = "list", description = "Lists modules in a pride")
public class ListCommand extends AbstractExistingPrideCommand {

	@Option(name = {"-m", "--modules"},
			description = "Show only the modules in the pride")
	private boolean explicitModules;

	@Option(name = {"-s", "--short"},
			description = "Show only module names")
	private boolean explicitShort;

	@Override
	public void runInPride(final Pride pride) {
		if (explicitShort || explicitModules) {
			for (Module module : pride.getModules()) {
				logger.info(ListCommand.formatModule(module, explicitShort));
			}
		} else {
			for (File dir : pride.rootDirectory.listFiles(new FileFilter() {
				@Override
				public boolean accept(File path) {
					return path.isDirectory();
				}
			})) {
				if (!Pride.isValidModuleDirectory(dir)) {
					continue;
				}

				if (pride.hasModule(dir.getName())) {
					logger.info(ListCommand.formatModule(pride.getModule(dir.getName()), false));
				} else {
					logger.info("? " + dir.getName());
				}
			}
		}

	}

	private static String formatModule(final Module module, boolean onlyNames) {
		return onlyNames ? module.getName() : "m " + module.getName() + " (" + module.getVcs().getType() + ")";
	}
}
