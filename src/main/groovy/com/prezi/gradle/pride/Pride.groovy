package com.prezi.gradle.pride

/**
 * Created by lptr on 10/04/14.
 */
class Pride {
	private final File rootDirectory
	private final File configDirectory

	public Pride(File rootDirectory) {
		this.rootDirectory = rootDirectory
		this.configDirectory = new File(rootDirectory, ".pride")
		if (!configDirectory.directory) {
			throw new PrideException("No pride in directory \"${rootDirectory}\"")
		}
	}

	public Set<File> getModules() {
		def modulesFile = new File(configDirectory, "modules")
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
