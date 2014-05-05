package com.prezi.gradle.pride

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 11/04/14.
 */
class PridePlugin implements Plugin<Project> {

	protected static final Logger logger = LoggerFactory.getLogger(PridePlugin)

	@Override
	void apply(Project project) {
		// Check if not running from the root of a Pride
		checkIfNotRunningFromRootOfPride(project)

		// Add our custom dependency declaration
		def extension = project.extensions.create("dynamicDependencies", DynamicDependenciesExtension, project)

		// Apply Pride convention
		project.convention.plugins.pride = new PrideConvention(project)

		resolveDynamicDependencies(project, extension)
	}

	private static resolveDynamicDependencies(Project project, extension) {
		project.gradle.projectsEvaluated {
			// Collect local projects in the session
			Map<String, Project> projectsByGroupAndName = project.rootProject.allprojects.collectEntries() { Project p ->
				[p.group + ":" + p.name, p]
			}
			logger.debug "Resolving dynamic dependencies among projects: ${projectsByGroupAndName.keySet()}"

			// Resolve dependencies to projects when possible
			extension.dependencies.each { Configuration configuration, Collection<Dependency> dependencies ->
				dependencies.each { Dependency dependency ->
					Dependency resolvedDependency = dependency
					if (dependency instanceof ExternalDependency) {
						logger.debug "Looking for ${dependency.group}:${dependency.name}"
						// See if we can resolve this external dependency to a project dependency
						def dependentProject = projectsByGroupAndName.get(dependency.group + ":" + dependency.name)
						if (dependentProject) {
							resolvedDependency = convertExternalToProjectDependency(project, dependency, dependentProject)
						}
					}

					// Add the resolved dependency
					configuration.dependencies.add(resolvedDependency)
				}
			}
		}
	}

	private
	static ProjectDependency convertExternalToProjectDependency(Project project, ExternalDependency externalDependency, Project dependentProject) {
		logger.debug "Resolved ${externalDependency.group}:${externalDependency.name} to ${dependentProject.path}"

		// Create project dependency
		String targetConfiguration = externalDependency instanceof ModuleDependency ? externalDependency.configuration : null
		def resolvedDependency = (ProjectDependency) project.dependencies.project(path: dependentProject.path, configuration: targetConfiguration)

		// Copy parameters from original dependency
		resolvedDependency.excludeRules.addAll(externalDependency.excludeRules)
		resolvedDependency.transitive = externalDependency.transitive
		externalDependency.artifacts.each { resolvedDependency.addArtifact(it) }

		return resolvedDependency
	}

	private static boolean alreadyCheckedIfRunningFromRootOfPride

	private static void checkIfNotRunningFromRootOfPride(Project project) {
		if (!alreadyCheckedIfRunningFromRootOfPride) {
			if (!Pride.containsPride(project.rootDir)) {
				logger.debug "No pride found in ${project.rootDir}"
				for (def dir = project.rootDir.parentFile; dir?.canRead(); dir = dir.parentFile) {
					logger.debug "Checking pride in $dir}"
					if (Pride.containsPride(dir)) {
						logger.warn "WARNING: Found a pride in parent directory ${dir}. " +
								"This means that you are running Gradle from a subproject of the pride. " +
								"Dynamic dependencies cannot be resolved to local projects this way. " +
								"To avoid this warning run Gradle from the root of the pride."
						break
					}
				}
			}
			alreadyCheckedIfRunningFromRootOfPride = true
		}
	}
}
