package com.prezi.gradle.pride

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by lptr on 11/04/14.
 */
class PridePlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		// Add our custom dependency declaration
		def moduleDependenciesExtension = project.extensions.create("moduleDependencies", ModulesDependenciesExtension, project)

		project.gradle.projectsEvaluated {
			project.logger.info "Resolving module dependencies for \"${project.path}\""
			def moduleDependencies = moduleDependenciesExtension.moduleDependencies
			if (moduleDependencies.empty) {
				return
			}

			// Collect local projects in the session
			Map<String, Project> allProjects = [:]
			project.rootProject.allprojects.each { Project p ->
				project.logger.debug "Found project: ${p.group} : ${p.name}"
				allProjects.put p.group + ":" + p.name, p
			}

			moduleDependencies.each { moduleDependency ->
				def referencedModule = allProjects.get(moduleDependency.group + ":" + moduleDependency.name)
				if (referencedModule) {
					project.logger.debug "Resolved module dependency ${moduleDependency} to local project"
					project.dependencies.add "modules", project.dependencies.project(path: referencedModule.path, configuration: moduleDependency.configuration)
				} else {
					project.logger.debug "Resolved module dependency ${moduleDependency} to external dependency"
					project.dependencies.add "modules", [group: moduleDependency.group, name: moduleDependency.name, version: moduleDependency.version, configuration: moduleDependency.configuration]
				}
			}
		}

		// Apply Pride convention
		project.convention.plugins.pride = new PrideConvention(project)
	}
}
