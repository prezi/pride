package com.prezi.gradle.pride.cli;

import com.google.common.collect.Iterators;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import com.prezi.gradle.pride.cli.gradle.GradleProjectExecution;
import com.prezi.gradle.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PrideInitializer {

	private static final Logger logger = LoggerFactory.getLogger(PrideInitializer.class);
	private static final String DO_NOT_MODIFY_WARNING =
			"//\n" +
			"// DO NOT MODIFY -- This file is generated by Pride, and will be\n" +
			"// overwritten whenever the pride itself is changed.\n//\n";
	private final GradleConnectorManager gradleConnectorManager;
	private final boolean verbose;

	public PrideInitializer(GradleConnectorManager gradleConnectorManager, boolean verbose) {
		this.gradleConnectorManager = gradleConnectorManager;
		this.verbose = verbose;
	}

	public Pride create(File prideDirectory, RuntimeConfiguration globalConfig, Configuration prideConfig, VcsManager vcsManager) throws IOException, ConfigurationException {
		logger.info("Initializing {}", prideDirectory);
		FileUtils.forceMkdir(prideDirectory);

		File configDirectory = Pride.getPrideConfigDirectory(prideDirectory);
		FileUtils.deleteDirectory(configDirectory);
		FileUtils.forceMkdir(configDirectory);
		FileUtils.write(Pride.getPrideVersionFile(configDirectory), "0\n");

		// Create config file
		File configFile = Pride.getPrideConfigFile(configDirectory);
		PropertiesConfiguration prideFileConfig = new PropertiesConfiguration(configFile);
		boolean prideConfigModified = false;
		for (String key : Iterators.toArray(prideConfig.getKeys(), String.class)) {
			// Skip modules
			if (key.startsWith("modules.")) {
				continue;
			}
			prideFileConfig.setProperty(key, prideConfig.getProperty(key));
			prideConfigModified = true;
		}
		// Override Gradle details
		if (gradleConnectorManager.setGradleConfiguration(prideFileConfig)) {
			prideConfigModified = true;
		}
		if (prideConfigModified) {
			prideFileConfig.save();
		}

		Pride pride = new Pride(prideDirectory, globalConfig, prideFileConfig, vcsManager);
		reinitialize(pride);
		return pride;
	}

	public void reinitialize(final Pride pride) {
		try {
			File buildFile = pride.getGradleBuildFile();
			FileUtils.deleteQuietly(buildFile);
			FileUtils.write(buildFile, DO_NOT_MODIFY_WARNING);
			FileOutputStream buildOut = new FileOutputStream(buildFile, true);
			try {
				IOUtils.copy(PrideInitializer.class.getResourceAsStream("/build.gradle"), buildOut);
			} finally {
				buildOut.close();
			}

			final File settingsFile = pride.getGradleSettingsFile();
			FileUtils.deleteQuietly(settingsFile);
			FileUtils.write(settingsFile, DO_NOT_MODIFY_WARNING);
			for (Module module : pride.getModules()) {
				File moduleDirectory = new File(pride.getRootDirectory(), module.getName());
				if (Pride.isValidModuleDirectory(moduleDirectory)) {
					initializeModule(pride, moduleDirectory, settingsFile);
				}
			}
		} catch (Exception ex) {
			throw new PrideException("There was a problem during the initialization of the pride. Fix the errors above, and try again with\n\n\tpride init --force", ex);
		}
	}

	private void initializeModule(final Pride pride, File moduleDirectory, final File settingsFile) {
		gradleConnectorManager.executeInProject(moduleDirectory, new GradleProjectExecution<Void, RuntimeException>() {
			@Override
			public Void execute(File moduleDirectory, ProjectConnection connection) {
				try {
					final String relativePath = pride.getRootDirectory().toURI().relativize(moduleDirectory.toURI()).toString();

					// Load the model for the build
					ModelBuilder<GradleBuild> builder = connection.model(GradleBuild.class);
					if (verbose) {
						builder.withArguments("--info", "--stacktrace");
					} else {
						builder.withArguments("-q");
					}
					final GradleBuild build = builder.get();

					// Merge settings
					FileUtils.write(settingsFile, "\n// Settings from project in directory /" + relativePath + "\n\n", true);
					for (BasicGradleProject project : build.getProjects()) {
						if (project.equals(build.getRootProject())) {
							FileUtils.write(settingsFile, "include \'" + build.getRootProject().getName() + "\'\n", true);
							FileUtils.write(settingsFile, "project(\':" + build.getRootProject().getName() + "\').projectDir = file(\'" + moduleDirectory.getName() + "\')\n", true);
						} else {
							FileUtils.write(settingsFile, "include \'" + build.getRootProject().getName() + project.getPath() + "\'\n", true);
						}
					}
					return null;
				} catch (Exception ex) {
					throw new PrideException("Could not parse module in " + moduleDirectory + ": " + ex, ex);
				}
			}
		});
	}
}
