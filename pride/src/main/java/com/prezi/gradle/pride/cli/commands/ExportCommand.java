package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.ExportedConfigurationHandler;
import com.prezi.gradle.pride.cli.ExportedModule;
import com.prezi.gradle.pride.vcs.VcsStatus;
import com.prezi.gradle.pride.vcs.VcsSupport;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@Command(name = "export", description = "Exports a pride")
public class ExportCommand extends AbstractPrideCommand {
	@Option(name = {"-o", "--output"},
			title = "file",
			description = "Output file; if none give, will print to standard out")
	private File output;

	@Option(name = {"-O", "--overwrite"},
			description = "Overwrite output file")
	private boolean overwrite;

	@Option(name = {"--explicit"},
			description = "Export explicit revisions instead of branches")
	private boolean explicit;

	@Override
	public void executeInPride(final Pride pride) throws Exception {
		if (!overwrite && output != null && output.exists()) {
			throw new PrideException("Output file already exists: " + output);
		}
		PropertiesConfiguration config = new PropertiesConfiguration();
		config.copy(pride.getLocalConfiguration());
		Collection<ExportedModule> exportedModules = Collections2.transform(pride.getModules(), new Function<Module, ExportedModule>() {
			@Override
			public ExportedModule apply(Module module) {
				try {
					VcsSupport vcsSupport = module.getVcs().getSupport();
					File moduleDirectory = pride.getModuleDirectory(module.getName());
					VcsStatus vcsStatus = vcsSupport.getStatus(moduleDirectory);
					String revision;
					if (explicit || Strings.isNullOrEmpty(vcsStatus.getBranch())) {
						revision = vcsStatus.getRevision();
					} else {
						revision = vcsStatus.getBranch();
					}
					return new ExportedModule(module.getName(), vcsSupport.getRepositoryUrl(moduleDirectory), revision, module.getVcs());
				} catch (IOException e) {
					throw Throwables.propagate(e);
				}
			}
		});
		ExportedConfigurationHandler configHandler = new ExportedConfigurationHandler(getVcsManager());
		configHandler.saveConfiguration(config, exportedModules);

		if (output == null) {
			config.save(System.out);
		} else {
			FileUtils.deleteQuietly(output);
			config.save(output);
		}
	}
}
