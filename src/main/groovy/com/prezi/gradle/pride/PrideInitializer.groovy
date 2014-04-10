package com.prezi.gradle.pride

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.gradle.GradleBuild

/**
 * Created by lptr on 31/03/14.
 */
class PrideInitializer {
	public static final String PRIDE_DIRECTORY = ".pride"
	public static final String PRIDE_MODULES = "modules"

	public static final String SETTINGS_GRADLE = "settings.gradle"
	public static final String BUILD_GRADLE = "build.gradle"
	public static final String GRADLE_PROPERTIES = "gradle.properties"

	private static ThreadLocal<GradleConnector> gradleConnector = new ThreadLocal<>() {
		@Override
		protected GradleConnector initialValue() {
			System.out.println("Starting Gradle connector")
			return GradleConnector.newConnector()
		}
	}

	public static void initializePride(File prideDirectory, boolean overwrite) {
		def settingsFile = new File(prideDirectory, SETTINGS_GRADLE)
		def buildFile = new File(prideDirectory, BUILD_GRADLE)
		def gradleProperties = new File(prideDirectory, GRADLE_PROPERTIES)
		def configDirectory = new File(prideDirectory, PRIDE_DIRECTORY)
		def prideModulesFile = new File(configDirectory, PRIDE_MODULES)

		def prideExists = configDirectory.exists() || settingsFile.exists() || buildFile.exists() || gradleProperties.exists()
		if (!overwrite && prideExists) {
			throw new PrideException("A pride already exists in ${prideDirectory}")
		}

		System.out.println((prideExists ? "Reinitializing" : "Initializing") + " ${prideDirectory}")
		prideDirectory.mkdirs()
		configDirectory.deleteDir()
		configDirectory.mkdirs()
		settingsFile.delete()
		buildFile.delete()
		gradleProperties.delete()

		prideDirectory.eachDir { moduleDirectory ->
			if (isValidProject(moduleDirectory)) {
				def connection = gradleConnector.get().forProjectDirectory(moduleDirectory).connect()
				try {
					def relativePath = prideDirectory.toURI().relativize(moduleDirectory.toURI()).toString()

					// Load the model for the build
					GradleBuild build = connection.getModel(GradleBuild)

					// Merge settings
					settingsFile << "\n// Settings from project in directory /${relativePath}\n\n"
					build.projects.each { project ->
						if (project == build.rootProject) {
							settingsFile << "include '${build.rootProject.name}'\n"
							settingsFile << "project(':${build.rootProject.name}').projectDir = file('${moduleDirectory.name}')\n"
						} else {
							settingsFile << "include '${build.rootProject.name}${project.path}'\n"
						}
					}

					// Merge gradle.properties
					def localGradleProperties = new File(moduleDirectory, GRADLE_PROPERTIES)
					if (localGradleProperties.exists()) {
						def localGradlePropertiesText = localGradleProperties.text
						if (!localGradlePropertiesText.endsWith("\n")) {
							localGradlePropertiesText += "\n"
						}
						gradleProperties << "\n# Properties from project in directory /${relativePath}\n\n"
						gradleProperties << localGradlePropertiesText
					}

					// Add module to .pride/modules
					prideModulesFile << "${moduleDirectory.name}\n"
				} finally {
					// Clean up
					connection.close()
				}
			}
		}

		// Add build.gradle with local version hack
		buildFile << getClass().getResourceAsStream("/build.gradle")
	}

	private static boolean isValidProject(File dir) {
		System.out.println("Scanning ${dir}")
		return !dir.name.startsWith(".") &&
				dir.list().contains(BUILD_GRADLE) ||
				dir.list().contains(SETTINGS_GRADLE)
	}
}
