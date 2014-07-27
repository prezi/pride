package com.prezi.gradle.pride.cli.gradle;

import com.google.common.base.Strings;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.FileConfiguration;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import static com.prezi.gradle.pride.cli.Configurations.GRADLE_HOME;
import static com.prezi.gradle.pride.cli.Configurations.GRADLE_VERSION;

public class GradleConnectorManager {
	private static final Logger logger = LoggerFactory.getLogger(GradleConnectorManager.class);

	private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)+(?:-.+)?");

	private final ThreadLocal<GradleConnector> gradleConnector;
	private final String gradleVersion;
	private final File gradleHome;

	public GradleConnectorManager(Configuration config) {
		this.gradleVersion = config.getString(GRADLE_VERSION);
		this.gradleHome = config.containsKey(GRADLE_HOME) ? new File(config.getString(GRADLE_HOME)) : null;
		this.gradleConnector = new ThreadLocal<GradleConnector>() {
			@Override
			protected GradleConnector initialValue() {
				logger.info("Starting Gradle connector");
				GradleConnector connector = GradleConnector.newConnector();
				if (!Strings.isNullOrEmpty(gradleVersion)) {
					if (VERSION_NUMBER_PATTERN.matcher(gradleVersion).matches()) {
						logger.debug("Using Gradle version {}", gradleVersion);
						connector.useGradleVersion(gradleVersion);
					} else {
						boolean validUri = false;
						try {
							URI gradleDistribution = new URI(gradleVersion);
							if (gradleDistribution.isAbsolute()) {
								validUri = true;
								logger.debug("Using Gradle distribution from {}", gradleDistribution);
								connector.useDistribution(gradleDistribution);
							}
						} catch (URISyntaxException e) {
							// Ignore
						}
						if (!validUri) {
							logger.debug("Could not parse as a valid URI, trying to use as installation: {}", gradleVersion);
							File gradleInstallation = new File(gradleVersion);
							logger.debug("Using Gradle installation {}", gradleInstallation);
							connector.useInstallation(gradleInstallation);
						}
					}
				}

				if (gradleHome != null) {
					logger.debug("Setting Gradle home to {}", gradleHome);
					connector.useGradleUserHomeDir(gradleHome);
				}
				return connector;
			}
		};
	}

	public GradleConnector getConnector() {
		return gradleConnector.get();
	}

	public <T, E extends Exception> T executeInProject(File projectDirectory, GradleProjectExecution<T, E> execution) throws E {
		ProjectConnection connection = getConnector().forProjectDirectory(projectDirectory).connect();
		try {
			return execution.execute(projectDirectory, connection);
		} finally {
			connection.close();
		}
	}

	public boolean setGradleConfiguration(FileConfiguration config) {
		boolean changed = false;
		if (!Strings.isNullOrEmpty(gradleVersion)) {
			config.setProperty(GRADLE_VERSION, gradleVersion);
			changed = true;
		}
		if (gradleHome != null) {
			config.setProperty(GRADLE_HOME, gradleHome);
			changed = true;
		}
		return changed;
	}
}
