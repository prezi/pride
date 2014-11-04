package com.prezi.gradle.pride.cli.commands.actions;

import com.google.common.base.Strings;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.ExportedConfigurationHandler;
import com.prezi.gradle.pride.cli.ExportedModule;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.config.ConfigurationData;
import com.prezi.gradle.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.net.URI;
import java.util.Collection;

public class InitActionFromImportedConfig extends InitActionBase {

	private final Collection<ExportedModule> modules;
	private final boolean useRepoCache;
	private final boolean noRepoCache;
	private final Boolean recursive;

	private InitActionFromImportedConfig(File prideDirectory, RuntimeConfiguration config, Configuration prideConfig, Collection<ExportedModule> modules, VcsManager vcsManager, boolean useRepoCache, boolean noRepoCache, Boolean recursive) {
		super(prideDirectory, config, prideConfig, vcsManager);
		this.modules = modules;
		this.useRepoCache = useRepoCache;
		this.noRepoCache = noRepoCache;
		this.recursive = recursive;
	}

	public static InitActionFromImportedConfig create(File prideDirectory, RuntimeConfiguration globalConfig, VcsManager vcsManager, String configLocation, boolean useRepoCache, boolean noRepoCache, Boolean recursive) throws Exception {
		boolean prideExistsAlready = Pride.containsPride(prideDirectory);
		if (prideExistsAlready) {
			throw new PrideException("Cannot create a pride from existing configuration in a directory that already contains a pride: "
					+ prideDirectory);
		}

		PropertiesConfiguration importedConfig;
		URI configUri = URI.create(configLocation);
		if (configUri.isAbsolute()) {
			importedConfig = new PropertiesConfiguration(configUri.toURL());
		} else {
			importedConfig = new PropertiesConfiguration(configUri.getRawPath());
		}

		ConfigurationData<ExportedModule> configurationData = new ExportedConfigurationHandler(vcsManager).loadConfiguration(importedConfig);
		for (ExportedModule exportedModule : configurationData.getModules()) {
			if (Strings.isNullOrEmpty(exportedModule.getRemote())) {
				throw new PrideException("No remote URL specified for module " + exportedModule.getName() + ". Please use `pride export` to generate a configuration that can be imported.");
			}
		}
		return new InitActionFromImportedConfig(prideDirectory, globalConfig, configurationData.getConfiguration(), configurationData.getModules(), vcsManager, useRepoCache, noRepoCache, recursive);
	}

	@Override
	protected void initPride(PrideInitializer prideInitializer, Pride pride, boolean verbose) throws Exception {
		// Add modules from imported config
		if (modules.isEmpty()) {
			return;
		}

		AddAction addAction = new AddAction(pride, false, useRepoCache, noRepoCache, recursive, verbose);
		addAction.addModules(modules);
	}
}
