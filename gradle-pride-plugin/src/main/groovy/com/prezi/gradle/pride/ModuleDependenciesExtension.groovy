package com.prezi.gradle.pride

import org.gradle.api.Project

/**
 * Created by lptr on 27/03/14.
 */
class ModulesDependenciesExtension {
	final def moduleDependencies = []
	private final Project project

	public ModulesDependenciesExtension(Project project) {
		this.project = project
	}

	public module(Map<String, String> params) {
		def group = params.group ?: project.group
		// TODO Fail if neither group nor version is specified
		def version = resolveVersion(params.version)
		def name = params.name
		def configuration = params.configuration ?: "modules"
		moduleDependencies.add([group: group, name: name, version: version, configuration: configuration])
	}

	private String resolveVersion(String version) {
		if (version) {
			// Custom handling of version: "2" case (single major version number)
			// To be removed after transition
			if (version ==~ /\d+/) {
				return version + ".+"
			}
			return version
		} else {
			return project.version
		}
	}
}
