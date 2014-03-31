package com.prezi.gradle.pride

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.gradle.GradleBuild

/**
 * Created by lptr on 31/03/14.
 */
class SessionInitializer {
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

	public static void initializeSession(File sessionDirectory, boolean overwrite) {
		def settingsFile = new File(sessionDirectory, SETTINGS_GRADLE)
		def buildFile = new File(sessionDirectory, BUILD_GRADLE)
		def gradleProperties = new File(sessionDirectory, GRADLE_PROPERTIES)

		def sessionExists = settingsFile.exists() || buildFile.exists() || gradleProperties.exists()
		if (!overwrite && sessionExists) {
			throw new PrideException("A session already exists in ${sessionDirectory}")
		}

		System.out.println((sessionExists ? "Reinitializing" : "Initializing") + " ${sessionDirectory}")
		sessionDirectory.mkdirs()
		settingsFile.delete()
		buildFile.delete()
		gradleProperties.delete()

		sessionDirectory.eachDir { dir ->
			if (isValidProject(dir)) {
				def connection = gradleConnector.get().forProjectDirectory(dir).connect()
				try {
					def relativePath = sessionDirectory.toURI().relativize(dir.toURI()).toString()

					// Load the model for the build
					GradleBuild build = connection.getModel(GradleBuild)

					// Merge settings
					settingsFile << "\n// Settings from project in directory /${relativePath}\n\n"
					build.projects.each { prj ->
						if (prj == build.rootProject) {
							settingsFile << "include '$build.rootProject.name'\n"
							settingsFile << "project(':$build.rootProject.name').projectDir = file('$dir.name')\n"
						} else {
							settingsFile << "include '$build.rootProject.name$prj.path'\n"
						}
					}

					// Merge gradle.properties
					def localGradleProperties = new File(dir, GRADLE_PROPERTIES)
					if (localGradleProperties.exists()) {
						def localGradlePropertiesText = localGradleProperties.text
						if (!localGradlePropertiesText.endsWith("\n")) {
							localGradlePropertiesText += "\n"
						}
						gradleProperties << "\n# Properties from project in directory /${relativePath}\n\n"
						gradleProperties << localGradlePropertiesText
					}
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
