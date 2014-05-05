package com.prezi.gradle.pride

import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
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

	private static void resolveDynamicDependencies(Project project, DynamicDependenciesExtension dynamicDependencies) {
		project.gradle.projectsEvaluated {
			// Collect local projects in the session
			Map<String, Project> projectsByGroupAndName = project.rootProject.allprojects.collectEntries() { Project p ->
				[p.group + ":" + p.name, p]
			}
			logger.debug "Resolving dynamic dependencies among projects: ${projectsByGroupAndName.keySet()}"

			dynamicDependencies.dependencies.collect { Configuration configuration, Collection<Dependency> dependencies ->
				// Localize dependencies to projects when possible
				LinkedHashSet<Dependency> localizedDependencies = dependencies.collect { Dependency dependency ->
					Dependency localizedDependency = dependency
					if (dependency instanceof ExternalDependency) {
						logger.debug "Looking for ${dependency.group}:${dependency.name}"
						// See if we can localize this external dependency to a project dependency
						def dependentProject = projectsByGroupAndName.get(dependency.group + ":" + dependency.name)
						if (dependentProject) {
							localizedDependency = convertExternalToProjectDependency(project, dependency, dependentProject)
						}
					}
					return localizedDependency
				}
				// Go through transitive dependencies and replace them with projects when applicable
				// See https://github.com/prezi/pride/issues/40
				def detachedConfiguration = project.configurations.detachedConfiguration(*localizedDependencies.toArray(new Dependency[0]))
				LinkedHashSet<ProjectOverride> projectOverrides = []
				detachedConfiguration.resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency resolvedDependency ->
					collectTransitiveDependenciesToOverride(projectsByGroupAndName, resolvedDependency.children, projectOverrides)
				}
				projectOverrides.collect(localizedDependencies) { ProjectOverride projectOverride ->
					project.logger.debug "Adding override project dependency: {}", projectOverride
					// This overrides the external dependency because project
					// versions are set to Short.MAX_VALUE in generated build.gradle
					project.dependencies.project(path: projectOverride.project.path, configuration: projectOverride.configuration)
				}

				configuration.dependencies.addAll localizedDependencies
			}
		}
	}

	private
	static ProjectDependency convertExternalToProjectDependency(Project project, ExternalDependency externalDependency, Project dependentProject) {
		String targetConfiguration = externalDependency instanceof ModuleDependency ? externalDependency.configuration : null
		logger.debug "Localizing ${externalDependency.group}:${externalDependency.name} to ${dependentProject.path}, configuration: ${targetConfiguration}"

		// Create project dependency
		def resolvedDependency = (ProjectDependency) project.dependencies.project(path: dependentProject.path, configuration: targetConfiguration)

		// Copy parameters from original dependency
		resolvedDependency.excludeRules.addAll(externalDependency.excludeRules)
		resolvedDependency.transitive = externalDependency.transitive
		externalDependency.artifacts.each { resolvedDependency.addArtifact(it) }

		return resolvedDependency
	}

	private static void collectTransitiveDependenciesToOverride(Map<String, Project> projectsByGroupAndName, Set<ResolvedDependency> dependencies, LinkedHashSet<ProjectOverride> projectOverrides) {
		dependencies.each { ResolvedDependency dependency ->
			def dependentProject = projectsByGroupAndName.get(dependency.moduleGroup + ":" + dependency.moduleName)
			if (dependentProject) {
				// Sometimes we get stuff that point to non-existent configurations like "master",
				// so we should skip those
				if (dependentProject.configurations.find({ it.name == dependency.configuration })) {
					projectOverrides.add(new ProjectOverride(dependency.configuration, dependentProject))
				}
			} else {
				// If a corresponding project is not found locally, traverse children of external dependency
				collectTransitiveDependenciesToOverride(projectsByGroupAndName, dependency.children, projectOverrides)
			}
		}
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

@TupleConstructor
@ToString(includeNames = true)
class ProjectOverride {
	String configuration
	Project project
}
