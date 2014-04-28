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
	final LinkedHashMap<Configuration, List<Dependency>> dependencies = [:]

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
		def dependenciesForConfig = dependencies.get(configuration)
		if (dependenciesForConfig == null) {
			dependenciesForConfig = []
			dependencies.put(configuration, dependenciesForConfig)
		}
		dependenciesForConfig.add(dependency)
		project.logger.debug("Added dynamic dependency ${dependency.group}:${dependency.name}:${dependency.version} (${dependency.getClass().name})")
		return dependency
	}
}
