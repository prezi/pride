package com.prezi.pride.cli.commands;

import com.google.common.base.Strings;
import com.prezi.pride.Module;
import com.prezi.pride.Pride;
import com.prezi.pride.vcs.VcsStatus;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.File;
import java.util.Collection;
import java.util.List;

@Command(name = "list", description = "List modules in a pride")
public class ListCommand extends AbstractFilteredPrideCommand {

	@Option(name = {"-I", "--include"},
			title = "regex",
			description = "Execute the command on module (can be specified multiple times)")
	private List<String> includeModules;

	@Option(name = {"-s", "--short"},
			description = "Show only module names")
	private boolean explicitShort;

	@Override
	protected void executeInModules(Pride pride, Collection<Module> modules) throws Exception {
		LineFormatter formatter;
		if (explicitShort) {
			formatter = new NamesOnlyFormatter();
		} else {
			formatter = new StatusFormatter(pride);
		}
		for (Module module : modules) {
			logger.info("{}", formatter.formatModule(module));
		}
	}

	@Override
	protected Collection<String> getIncludeModules() {
		return includeModules;
	}

	private interface LineFormatter {
		String formatModule(Module module) throws Exception;
	}

	private static class NamesOnlyFormatter implements LineFormatter {

		@Override
		public String formatModule(Module module) {
			return module.getName();
		}
	}

	private static class StatusFormatter implements LineFormatter {

		private final Pride pride;

		private StatusFormatter(Pride pride) {
			this.pride = pride;
		}

		@Override
		public String formatModule(Module module) throws Exception {
			File moduleDirectory = pride.getModuleDirectory(module.getName());
			// MM module-name (git)
			VcsStatus status = module.getVcs().getSupport().getStatus(moduleDirectory);
			String branch = status.getBranch();
			StringBuilder line = new StringBuilder();
			line.append(status.hasUnpublishedChanges() ? 'M' : ' ');
			line.append(status.hasUncommittedChanges() ? 'M' : ' ');
			line.append(' ').append(module.getName());
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
