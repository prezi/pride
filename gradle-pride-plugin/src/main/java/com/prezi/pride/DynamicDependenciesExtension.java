package com.prezi.pride;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DynamicDependenciesExtension extends GroovyObjectSupport {

	private static final Logger logger = LoggerFactory.getLogger(DynamicDependenciesExtension.class);
	private final Project project;
	private final Map<String, Project> projectsByGroupAndName;
	private final SetMultimap<String, Dependency> dynamicDependencies = LinkedHashMultimap.create();
	private final SetMultimap<String, Dependency> requestedDynamicDependencies = LinkedHashMultimap.create();

	public DynamicDependenciesExtension(Project project, Map<String, Project> projectsByGroupAndName) {
		this.project = project;
		this.projectsByGroupAndName = projectsByGroupAndName;
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

	public void add(final Configuration configuration, Dependency dependency) {
		project.getLogger().debug("Adding dynamic dependency " + dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion() + " (" + dependency.getClass().getName() + ")");
		requestedDynamicDependencies.put(configuration.getName(), dependency);
		final Dependency localizedDependency;
		if (PridePlugin.isDisabled(project)) {
			logger.info("Dynamic dependency resolution is disabled, all dynamic dependencies will be resolved to external dependencies");
			localizedDependency = dependency;
		} else {
			localizedDependency = localizeFirstLevelDynamicDependency(dependency);
			dynamicDependencies.put(configuration.getName(), localizedDependency);
		}

		// Defer adding project dependency until it is evaluated
		Action<Project> addDependencyAction = new Action<Project>() {
			@Override
			public void execute(Project project) {
				configuration.getDependencies().add(localizedDependency);
			}
		};
		if (localizedDependency instanceof ProjectDependency) {
			ProjectInternal dependencyProject = (ProjectInternal) ((ProjectDependency) localizedDependency).getDependencyProject();
			if (!dependencyProject.getState().getExecuted()) {
				dependencyProject.afterEvaluate(addDependencyAction);
				return;
			}
		}
		// If dependency project is evaluated, or it's not a project dependency, add it straight away
		addDependencyAction.execute(null);
	}

	private Dependency localizeFirstLevelDynamicDependency(Dependency dependency) {
		Dependency localizedDependency = dependency;
		if (dependency instanceof ExternalDependency) {
			logger.debug("Looking for {}:{}", dependency.getGroup(), dependency.getName());
			ExternalDependency externalDependency = (ExternalDependency) dependency;
			// See if we can localize this external dependency to a project dependency
			Project dependentProject = projectsByGroupAndName.get(dependency.getGroup() + ":" + dependency.getName());
			if (dependentProject != null) {
				final String targetConfiguration = externalDependency.getConfiguration();
				logger.debug("Localizing {}:{} to {}, configuration: {}", dependency.getGroup(), dependency.getName(), dependentProject, targetConfiguration);
				ProjectDependency projectDependency = (ProjectDependency) project.getDependencies().project(
						ImmutableMap.of(
								"path", dependentProject.getPath(),
								"configuration", targetConfiguration
						)
				);
				projectDependency.setTransitive(externalDependency.isTransitive());
				projectDependency.getExcludeRules().addAll(externalDependency.getExcludeRules());
				projectDependency.getArtifacts().addAll(externalDependency.getArtifacts());
				localizedDependency = projectDependency;
			}
		}
		return localizedDependency;
	}

	@SuppressWarnings("unchecked")
	private Dependency doAdd(Configuration configuration, Object dependencyNotation, Closure closure) {
		// Let the DependencyHandler parse our dependency definition
		Dependency dependency = project.getDependencies().create(dependencyNotation, closure);
		add(configuration, dependency);
		return dependency;
	}

	public Map<String, Collection<Dependency>> getDynamicDependencies() {
		return Collections.unmodifiableMap(dynamicDependencies.asMap());
	}

	public Map<String, Collection<Dependency>> getRequestedDynamicDependencies() {
		return Collections.unmodifiableMap(requestedDynamicDependencies.asMap());
	}
}
