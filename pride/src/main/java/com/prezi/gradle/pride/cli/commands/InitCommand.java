package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.CliConfiguration;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.vcs.Vcs;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

@Command(name = "init", description = "Initialize pride")
public class InitCommand extends AbstractPrideCommand {

	@Option(name = {"-f", "--force"},
			description = "Force initialization of a pride, even if one already exists")
	private boolean overwrite;

	@Option(name = {"-T", "--repo-type"},
			title = "type",
			description = "Repository type (used to identify the type of any existing repos)")
	private String explicitRepoType;

	@Option(name = "--no-add-existing",
			description = "Do not add existing modules in the pride directory to the pride")
	private boolean explicitNoAddExisting;

	@Override
	protected void runInternal() throws IOException {
		if (!overwrite && Pride.containsPride(getPrideDirectory())) {
			throw new PrideException("A pride already exists in " + getPrideDirectory());
		}

		final Pride pride = PrideInitializer.create(getPrideDirectory(), getConfiguration(), getVcsManager());
		final Vcs vcs = getVcs();

		if (!explicitNoAddExisting) {
			logger.debug("Adding existing modules");
			boolean addedAny = false;
			for (File dir : getPrideDirectory().listFiles(new FileFilter() {
				@Override
				public boolean accept(File path) {
					return path.isDirectory();
				}
			})) {
				if (Pride.isValidModuleDirectory(dir)) {
					logger.info("Adding existing " + vcs.getType() + " module in " + dir);
					pride.addModule(dir.getName(), vcs);
					addedAny = true;
				}
			}
			if (addedAny) {
				pride.save();
				PrideInitializer.reinitialize(pride);
			}
		}
	}

	@Override
	protected void overrideConfiguration(Configuration configuration) {
		super.overrideConfiguration(configuration);
		if (!StringUtils.isEmpty(explicitRepoType)) {
			configuration.setProperty(CliConfiguration.REPO_TYPE_DEFAULT, explicitRepoType);
		}
	}
}
