package com.prezi.gradle.pride;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicDependenciesExtension extends GroovyObjectSupport {

	private final Project project;
	private final LinkedHashMap<Configuration, List<Dependency>> dependencies = new LinkedHashMap<Configuration, List<Dependency>>();

	public DynamicDependenciesExtension(Project project) {
		this.project = project;
	}

	// Copied over mostly intact from DefaultDependencyHandler (1.12)
	@SuppressWarnings("UnusedDeclaration")
	public Object methodMissing(String name, Object args) {
		Object[] argsArray = (Object[]) args;
		Configuration configuration = project.getConfigurations().findByName(name);
		if (configuration == null) {
			throw new MissingMethodException(name, this.getClass(), argsArray);
		}

		List<?> normalizedArgs = CollectionUtils.flattenCollections(argsArray);
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

	@SuppressWarnings("unchecked")
	private Dependency doAdd(Configuration configuration, Object dependencyNotation, Closure closure) {
		// When not set, group and version should come from the current project
		if (dependencyNotation instanceof Map) {
			dependencyNotation = new LinkedHashMap((Map) dependencyNotation);
			if (!((LinkedHashMap) dependencyNotation).containsKey("group")) {
				((LinkedHashMap) dependencyNotation).put("group", project.getGroup());
			}

			if (!((LinkedHashMap) dependencyNotation).containsKey("version")) {
				((LinkedHashMap) dependencyNotation).put("version", project.getVersion());
			}
		}

		// Let the DependencyHandler parse our dependency definition
		Dependency dependency = project.getDependencies().create(dependencyNotation, closure);
		List<Dependency> dependenciesForConfig = dependencies.get(configuration);
		if (dependenciesForConfig == null) {
			dependenciesForConfig = new ArrayList();
			dependencies.put(configuration, dependenciesForConfig);
		}

		dependenciesForConfig.add(dependency);
		project.getLogger().debug("Added dynamic dependency " + dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion() + " (" + dependency.getClass().getName() + ")");
		return dependency;
	}

	public final LinkedHashMap<Configuration, List<Dependency>> getDependencies() {
		return dependencies;
	}

}
