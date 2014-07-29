package com.prezi.gradle.pride.cli.commands.actions;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.ModuleAdder;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;

public class InitActionFromImportedConfig extends InitActionBase {

	private InitActionFromImportedConfig(File prideDirectory, RuntimeConfiguration config, Configuration prideConfig, VcsManager vcsManager) {
		super(prideDirectory, config, prideConfig, vcsManager);
	}

	public static InitActionFromImportedConfig create(File prideDirectory, RuntimeConfiguration globalConfig, VcsManager vcsManager, String configLocation) throws Exception {
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
		return new InitActionFromImportedConfig(prideDirectory, globalConfig, importedConfig, vcsManager);
	}

	@Override
	protected int initPride(PrideInitializer prideInitializer, Pride pride) throws Exception {
		// Add modules from imported config
		List<Module> modulesFromConfiguration = Pride.getModulesFromConfiguration(prideConfig, vcsManager);
		if (modulesFromConfiguration.isEmpty()) {
			return 0;
		}

		Collection<String> moduleNames = Collections2.transform(modulesFromConfiguration, new Function<Module, String>() {
			@Override
			public String apply(Module module) {
				return module.getName();
			}
		});
		List<String> failedModules = ModuleAdder.addModules(pride, moduleNames, vcsManager);
		saveAndReinitializePride(prideInitializer, pride);
		return failedModules.isEmpty() ? 0 : 1;
	}
}
