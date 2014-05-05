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

		localizeDynamicDependencies(project, extension)
	}

	private static void localizeDynamicDependencies(Project project, DynamicDependenciesExtension dynamicDependencies) {
		project.gradle.projectsEvaluated {
			Map<String, Project> projectPathsByGroupAndName = project.rootProject.allprojects.collectEntries() { Project p ->
				[p.group + ":" + p.name, p]
			}

			// Collect local projects in the session
			logger.debug "Resolving dynamic dependencies among projects: ${projectPathsByGroupAndName.keySet()}"
			dynamicDependencies.dependencies.collect { Configuration configuration, Collection<Dependency> dependencies ->
				def localizedDependencies = localizeDynamicDependencies(dependencies, project, projectPathsByGroupAndName)
				configuration.dependencies.addAll localizedDependencies
			}
		}
	}

	private
	static Set<Dependency> localizeDynamicDependencies(Collection<Dependency> dependencies, Project project, Map<String, Project> projectsByGroupAndName) {
		// Localize first-level dependencies first
		def localProjectResolver = new DefaultLocalProjectResolver(project)
		def projectPathsByGroupAndName = projectsByGroupAndName.collectEntries(new LinkedHashMap<String, String>()) { String id, Project p ->
			[id, p.path]
		}
		LinkedHashSet<Dependency> localizedDependencies = dependencies.collect { Dependency dependency ->
			return localizeFirstLevelDynamicDependency(dependency, projectPathsByGroupAndName, localProjectResolver)
		}

		// Go through transitive dependencies and replace them with projects when applicable
		// See https://github.com/prezi/pride/issues/40
		def detachedConfiguration = project.configurations.detachedConfiguration(*localizedDependencies.toArray(new Dependency[0]))
		LinkedHashSet<ProjectOverride> projectOverrides = []
		detachedConfiguration.resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency resolvedDependency ->
			collectTransitiveDependenciesToOverride(projectsByGroupAndName, resolvedDependency.children, projectOverrides)
		}
		projectOverrides.collect(localizedDependencies) { ProjectOverride projectOverride ->
			logger.debug "Adding override project dependency: {}", projectOverride
			// This overrides the external dependency because project
			// versions are set to Short.MAX_VALUE in generated build.gradle
			project.dependencies.project(path: projectOverride.project.path, configuration: projectOverride.configuration)
		}
		return localizedDependencies
	}

	static Dependency localizeFirstLevelDynamicDependency(Dependency dependency, Map<String, String> projectPathsByGroupAndName, LocalProjectResolver projectResolver) {
		Dependency localizedDependency = dependency
		if (dependency instanceof ExternalDependency) {
			logger.debug "Looking for ${dependency.group}:${dependency.name}"
			// See if we can localize this external dependency to a project dependency
			def dependentProjectPath = projectPathsByGroupAndName.get(dependency.group + ":" + dependency.name)
			if (dependentProjectPath) {
				String targetConfiguration = dependency instanceof ModuleDependency ? dependency.configuration : null
				logger.debug "Localizing ${dependency.group}:${dependency.name} to ${dependentProjectPath}, configuration: ${targetConfiguration}"
				def projectDependency = projectResolver.resolveLocalProject(dependentProjectPath, targetConfiguration)
				projectDependency.transitive = dependency.transitive
				projectDependency.excludeRules.addAll(dependency.excludeRules)
				projectDependency.artifacts.addAll(dependency.artifacts)
				localizedDependency = projectDependency
			}
		}
		return localizedDependency
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

interface LocalProjectResolver {
	ProjectDependency resolveLocalProject(String path, String configuration)
}

class DefaultLocalProjectResolver implements LocalProjectResolver {

	private final Project project

	DefaultLocalProjectResolver(Project project) {
		this.project = project
	}

	@Override
	ProjectDependency resolveLocalProject(String path, String configuration) {
		(ProjectDependency) project.dependencies.project(path: path, configuration: configuration)
	}
}
