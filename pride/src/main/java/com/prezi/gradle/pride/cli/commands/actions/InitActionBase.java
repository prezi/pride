package com.prezi.gradle.pride.cli.commands.actions;

import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.PrideInitializer;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import com.prezi.gradle.pride.cli.gradle.GradleProjectExecution;
import com.prezi.gradle.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class InitActionBase {
	private static final Logger logger = LoggerFactory.getLogger(InitActionBase.class);

	private final File prideDirectory;
	protected final RuntimeConfiguration globalConfig;
	protected final Configuration prideConfig;
	protected final VcsManager vcsManager;

	protected InitActionBase(File prideDirectory, RuntimeConfiguration globalConfig, Configuration prideConfig, VcsManager vcsManager) {
		this.prideDirectory = prideDirectory;
		this.globalConfig = globalConfig;
		this.prideConfig = prideConfig;
		this.vcsManager = vcsManager;
	}

	public final int createPride(boolean addWrapper) throws Exception {
		// Make sure we take the local config into account when choosing the Gradle installation
		RuntimeConfiguration configForGradle = globalConfig.withConfiguration(prideConfig);
		GradleConnectorManager gradleConnectorManager = new GradleConnectorManager(configForGradle);

		// Create the pride
		PrideInitializer prideInitializer = new PrideInitializer(gradleConnectorManager);
		Pride pride = prideInitializer.create(prideDirectory, globalConfig, prideConfig, vcsManager);

		if (addWrapper) {
			logger.info("Adding Gradle wrapper");
			gradleConnectorManager.executeInProject(pride.getRootDirectory(), new GradleProjectExecution<Void, RuntimeException>() {
				@Override
				public Void execute(File projectDirectory, ProjectConnection connection) {
					connection.newBuild()
							.forTasks("wrapper")
							.run();
					return null;
				}
			});
		}

		return initPride(prideInitializer, pride);
	}

	protected static void saveAndReinitializePride(PrideInitializer prideInitializer, Pride pride) throws Exception {
		pride.save();
		prideInitializer.reinitialize(pride);
	}

	abstract protected int initPride(PrideInitializer prideInitializer, Pride pride) throws Exception;
}
