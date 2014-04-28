package com.prezi.gradle.pride

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ModuleDependency

/**
 * Created by lptr on 25/04/14.
 */
class DynamicDependenciesExtension {
	private final Project project

	public DynamicDependenciesExtension(Project project) {
		this.project = project
	}

	// Copied over mostly intact from DefaultDependencyHandler (1.11)
	def methodMissing(String name, args) {
		Object[] argsArray = (Object[]) args;
		Configuration configuration = project.configurations.findByName(name);
		if (configuration == null) {
			throw new MissingMethodException(name, this.getClass(), argsArray);
		}

		List<?> normalizedArgs = argsArray.flatten();
		if (normalizedArgs.size() == 2 && normalizedArgs.get(1) instanceof Closure) {
			return doAdd(configuration, normalizedArgs.get(0), (Closure) normalizedArgs.get(1));
		} else if (normalizedArgs.size() == 1) {
			return doAdd(configuration, normalizedArgs.get(0), null);
		} else {
			for (Object arg : normalizedArgs) {
				doAdd(configuration, arg, null);
			}
			return null;
		}
	}

	def relativeProject(Map<String, ?> notation) {
		return project.dependencies.project(PrideConvention.resolveProjectPath(project, notation))
	}

	private Dependency doAdd(Configuration configuration, Object dependencyNotation, Closure closure) {
		// When not set, group and version should come from the current project
		if (dependencyNotation instanceof Map) {
			dependencyNotation = new LinkedHashMap<>(dependencyNotation)
			if (!dependencyNotation.containsKey("group")) {
				dependencyNotation.put("group", project.group)
			}
			if (!dependencyNotation.containsKey("version")) {
				dependencyNotation.put("version", project.version)
			}
		}

		// Let the DependencyHandler parse our dependency definition
		def dependency = project.dependencies.create(dependencyNotation, closure)

		Dependency resolvedDependency = dependency
		if (dependency instanceof ExternalDependency) {
			// See if we can resolve this external dependency to a project dependency
			def project = project.rootProject.allprojects.find { it.group == dependency.group && it.name == dependency.name }
			if (project) {
				String targetConfiguration = dependency instanceof ModuleDependency ? dependency.configuration : null
				resolvedDependency = project.dependencies.project(path: project.path, configuration: targetConfiguration)
			}
		}

		// Add the resolved dependency
		configuration.dependencies.add(resolvedDependency)
		return resolvedDependency
	}
}
