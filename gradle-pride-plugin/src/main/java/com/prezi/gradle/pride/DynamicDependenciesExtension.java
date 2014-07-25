package com.prezi.gradle.pride;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.util.CollectionUtils;

import java.util.List;
import java.util.Map;

public class DynamicDependenciesExtension extends GroovyObjectSupport {

	private final Project project;
	private final Map<String, List<Dependency>> dependencies = Maps.newLinkedHashMap();

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

	public void add(Configuration configuration, Dependency dependency) {
		List<Dependency> dependenciesForConfig = dependencies.get(configuration.getName());
		if (dependenciesForConfig == null) {
			dependenciesForConfig = Lists.newArrayList();
			dependencies.put(configuration.getName(), dependenciesForConfig);
		}

		dependenciesForConfig.add(dependency);
		project.getLogger().debug("Added dynamic dependency " + dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion() + " (" + dependency.getClass().getName() + ")");
	}

	@SuppressWarnings("unchecked")
	private Dependency doAdd(Configuration configuration, Object dependencyNotation, Closure closure) {
		// When not set, group and version should come from the current project
		if (dependencyNotation instanceof Map) {
			dependencyNotation = Maps.newLinkedHashMap((Map) dependencyNotation);
			if (!((Map) dependencyNotation).containsKey("group")) {
				((Map) dependencyNotation).put("group", project.getGroup());
			}

			if (!((Map) dependencyNotation).containsKey("version")) {
				((Map) dependencyNotation).put("version", project.getVersion());
			}
		}

		// Let the DependencyHandler parse our dependency definition
		Dependency dependency = project.getDependencies().create(dependencyNotation, closure);
		add(configuration, dependency);
		return dependency;
	}

	public Map<String, List<Dependency>> getDependencies() {
		return dependencies;
	}

}
