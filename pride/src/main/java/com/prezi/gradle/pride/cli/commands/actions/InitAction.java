package com.prezi.gradle.pride.cli.commands.actions;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsManager;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Map;

public class InitAction extends InitActionBase {
	private static final Logger logger = LoggerFactory.getLogger(InitAction.class);

	private final Map<String, Module> modulesFromExistingPrideConfig;
	private final boolean addExisting;

	private InitAction(File prideDirectory, RuntimeConfiguration config, Configuration prideConfig, VcsManager vcsManager, Map<String, Module> modulesFromExistingPrideConfig, boolean addExisting) {
		super(prideDirectory, config, prideConfig, vcsManager);
		this.modulesFromExistingPrideConfig = modulesFromExistingPrideConfig;
		this.addExisting = addExisting;
	}

	public static InitAction create(File prideDirectory, RuntimeConfiguration globalConfig, VcsManager vcsManager, boolean force, boolean addExisting, boolean ignoreExistingConfig) throws Exception {
		boolean prideExistsAlready = Pride.containsPride(prideDirectory);
		if (prideExistsAlready && !force) {
			throw new PrideException("A pride already exists in " + prideDirectory);
		}

		Configuration existingPrideConfig = new BaseConfiguration();
		Map<String, Module> modulesFromExistingPrideConfig = Collections.emptyMap();

		if (prideExistsAlready && !ignoreExistingConfig) {
			try {
				Pride pride = Pride.getPride(prideDirectory, globalConfig, vcsManager);
				existingPrideConfig = pride.getLocalConfiguration();
				// Get existing modules
				if (addExisting) {
					modulesFromExistingPrideConfig = Maps.uniqueIndex(pride.getModules(), new Function<Module, String>() {
						@Override
						public String apply(Module module) {
							return module.getName();
						}
					});
				}
			} catch (Exception ex) {
				logger.warn("Could not load existing pride, ignoring existing configuration");
				logger.debug("Exception was", ex);
			}
		}
		return new InitAction(prideDirectory, globalConfig, existingPrideConfig, vcsManager, modulesFromExistingPrideConfig, addExisting);
	}

	@Override
	protected int initPride(PrideInitializer prideInitializer, Pride pride) throws Exception {
		boolean prideModified = false;
		if (addExisting) {
			logger.debug("Adding existing modules");
			for (File dir : pride.getRootDirectory().listFiles(new FileFilter() {
				@Override
				public boolean accept(File path) {
					return path.isDirectory();
				}
			})) {
				String moduleName = dir.getName();
				String repositoryUrl;
				Vcs vcs;

				Module existingModule = modulesFromExistingPrideConfig.get(moduleName);
				if (existingModule != null) {
					logger.info("Found existing module from previous configuration: {}", moduleName);
					repositoryUrl = existingModule.getRemote();
					vcs = existingModule.getVcs();
				} else if (Pride.isValidModuleDirectory(dir)) {
					vcs = vcsManager.findSupportingVcs(dir, globalConfig);
					repositoryUrl = vcs.getSupport().getRepositoryUrl(dir);
					if (repositoryUrl == null) {
						throw new PrideException("Could not detect remote URL for " + dir);
					}
				} else {
					continue;
				}
				logger.info("Adding existing {} module in {}", vcs.getType(), dir);
				pride.addModule(moduleName, repositoryUrl, vcs);
				prideModified = true;
			}
		}
		if (prideModified) {
			saveAndReinitializePride(prideInitializer, pride);
		}
		return 0;
	}
}
