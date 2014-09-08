package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Strings;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.vcs.VcsStatus;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.File;
import java.io.FileFilter;

@Command(name = "list", description = "List modules in a pride")
public class ListCommand extends AbstractPrideCommand {

	@Option(name = {"-m", "--modules"},
			description = "Show only the modules in the pride")
	private boolean explicitModules;

	@Option(name = {"-s", "--short"},
			description = "Show only module names")
	private boolean explicitShort;

	@Override
	public void executeInPride(Pride pride) throws Exception {
		AbstractLineFormatter formatter;
		if (explicitShort) {
			formatter = new NamesOnlyFormatter(pride, explicitModules);
		} else {
			formatter = new StatusFormatter(pride, explicitModules);
		}
		for (File dir : pride.getRootDirectory().listFiles(new FileFilter() {
			@Override
			public boolean accept(File path) {
				return path.isDirectory();
			}
		})) {
			if (!Pride.isValidModuleDirectory(dir)) {
				continue;
			}
			String line = formatter.formatDirectory(dir.getName());
			if (line != null) {
				logger.info("{}", line);
			}
		}
	}

	private static abstract class AbstractLineFormatter {
		protected final Pride pride;
		private final boolean onlyModules;

		protected AbstractLineFormatter(Pride pride, boolean onlyModules) {
			this.pride = pride;
			this.onlyModules = onlyModules;
		}

		public String formatDirectory(String name) throws Exception {
			if (!pride.hasModule(name)) {
				if (onlyModules) {
					return null;
				}
				return "? " + name;
			} else {
				return formatModule(name);
			}
		}

		protected abstract String formatModule(String name) throws Exception;
	}

	private static class NamesOnlyFormatter extends AbstractLineFormatter {

		protected NamesOnlyFormatter(Pride pride, boolean onlyModules) {
			super(pride, onlyModules);
		}

		@Override
		protected String formatModule(String name) {
			return name;
		}
	}

	private static class StatusFormatter extends AbstractLineFormatter {

		protected StatusFormatter(Pride pride, boolean onlyModules) {
			super(pride, onlyModules);
		}

		@Override
		protected String formatModule(String name) throws Exception {
			Module module = pride.getModule(name);
			File moduleDirectory = pride.getModuleDirectory(name);
			// MM module-name (git)
			VcsStatus status = module.getVcs().getSupport().getStatus(moduleDirectory);
			String branch = status.getBranch();
			StringBuilder line = new StringBuilder();
			line.append(status.hasUnpublishedChanges() ? 'M' : ' ');
			line.append(status.hasUncommittedChanges() ? 'M' : ' ');
			line.append(' ').append(name);
			line.append(' ');
			if (!Strings.isNullOrEmpty(branch)) {
				line.append(branch).append('@');
			}
			line.append(status.getRevision());
			line.append(" (").append(module.getVcs().getType()).append(")");
			return line.toString();
		}
	}
}
