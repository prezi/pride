package com.prezi.gradle.pride

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 10/04/14.
 */
class Pride {
	private static final Logger log = LoggerFactory.getLogger(Pride)

	public static final String PRIDE_DIRECTORY = ".pride"
	public static final String PRIDE_MODULES = "modules"
	public static final String PRIDE_VERSION = "version"

	public final File rootDirectory
	private final File configDirectory

	public static Pride lookupPride(File directory) {
		if (containsPride(directory)) {
			return new Pride(directory)
		} else {
			def parent = directory.parentFile
			if (parent) {
				return lookupPride(parent)
			} else {
				return null
			}
		}
	}

	public static Pride getPride(File directory) {
		def pride = lookupPride(directory)
		if (pride == null) {
			throw new PrideException("No pride found in ${directory}")
		}
		return pride
	}

	public static boolean containsPride(File directory) {
		def versionFile = new File(new File(directory, PRIDE_DIRECTORY), PRIDE_VERSION)
		def result = versionFile.exists() && versionFile.text == "0\n"
		log.debug "Directory ${directory} contains a pride: ${result}"
		return result
	}

	private Pride(File rootDirectory) {
		this.rootDirectory = rootDirectory
		this.configDirectory = new File(rootDirectory, PRIDE_DIRECTORY)
		if (!configDirectory.directory) {
			throw new PrideException("No pride in directory \"${rootDirectory}\"")
		}
	}

	public Set<File> getModules() {
		def modulesFile = new File(configDirectory, PRIDE_MODULES)
		if (!modulesFile.exists()) {
			throw new PrideException("Cannot find modules file in ${configDirectory}")
		}
		def moduleDirs = modulesFile.readLines().collectMany { String line ->
			def moduleName = line.trim()
			if (moduleName.empty || moduleName.startsWith("#")) {
				return []
			}
			def moduleDir = new File(rootDirectory, moduleName)
			if (!moduleDir.directory) {
				throw new PrideException("Module \"${moduleName}\" is missing")
			}

			return [ moduleDir ]
		}
		return moduleDirs
	}
}
