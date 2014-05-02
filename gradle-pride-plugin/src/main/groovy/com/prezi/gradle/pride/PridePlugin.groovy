package com.prezi.gradle.pride

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ModuleDependency

/**
 * Created by lptr on 11/04/14.
 */
class PridePlugin implements Plugin<Project> {

	static boolean alreadyWarnedUserAboutNotRunningFromRootOfPride

	@Override
	void apply(Project project) {
		// Check if not running from the root of a Pride
		if (!alreadyWarnedUserAboutNotRunningFromRootOfPride) {
			project.logger.info "Running from ${project.rootDir}"

			if (!Pride.containsPride(project.rootDir)) {
				project.logger.info "No pride found in ${project.rootDir}"
				for (def dir = project.rootDir.parentFile; dir?.canRead(); dir = dir.parentFile) {
					project.logger.info "Checking pride in $dir}"
					if (Pride.containsPride(dir)) {
						project.logger.warn "WARNING: Found a pride in parent directory ${dir}. " +
								"This means that you are running Gradle from a subproject of the pride. " +
								"Dynamic dependencies cannot be resolved to local projects this way. " +
								"To avoid this warning run Gradle from the root of the pride."
						alreadyWarnedUserAboutNotRunningFromRootOfPride = true
						break
					}
				}
			}
		}

		// Add our custom dependency declaration
		def extension = project.extensions.create("dynamicDependencies", DynamicDependenciesExtension, project)

		// Apply Pride convention
		project.convention.plugins.pride = new PrideConvention(project)

		project.gradle.projectsEvaluated {
			// Collect local projects in the session
			Map<String, Project> projectsByGroupAndName = project.rootProject.allprojects.collectEntries() { Project p ->
				[p.group + ":" + p.name, p]
			}
			project.logger.debug("Resolving dynamic dependencies among projects: ${projectsByGroupAndName.keySet()}")

			// Resolve dependencies to projects when possible
			extension.dependencies.each { Configuration configuration, Collection<Dependency> dependencies ->
				dependencies.each { Dependency dependency ->
					Dependency resolvedDependency = dependency
					if (dependency instanceof ExternalDependency) {
						project.logger.debug "Looking for ${dependency.group}:${dependency.name}"
						// See if we can resolve this external dependency to a project dependency
						def depProject = projectsByGroupAndName.get(dependency.group + ":" + dependency.name)
						if (depProject) {
							project.logger.debug "Resolved ${dependency.group}:${dependency.name} to ${depProject.path}"
							String targetConfiguration = dependency instanceof ModuleDependency ? dependency.configuration : null
							resolvedDependency = depProject.dependencies.project(path: depProject.path, configuration: targetConfiguration)
						}
					}

					// Add the resolved dependency
					configuration.dependencies.add(resolvedDependency)
				}
			}
		}
	}
}
