package com.prezi.gradle.pride;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
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
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DynamicDependenciesExtension extends GroovyObjectSupport {

	private static final Logger logger = LoggerFactory.getLogger(DynamicDependenciesExtension.class);
	private final Project project;
	private final Map<String, Project> projectsByGroupAndName;
	private final SetMultimap<Configuration, Dependency> dynamicDependencies = LinkedHashMultimap.create();

	public DynamicDependenciesExtension(Project project, final Map<String, Project> projectsByGroupAndName) throws IOException {
		this.project = project;
		this.projectsByGroupAndName = projectsByGroupAndName;

		// Go through transitive dependencies and replace them with projects when applicable
		// See https://github.com/prezi/pride/issues/40
		project.afterEvaluate(new Action<Project>() {
			@Override
			public void execute(Project project) {
				for (Map.Entry<Configuration, Collection<Dependency>> entry : dynamicDependencies.asMap().entrySet()) {
					Configuration configuration = entry.getKey();
					Collection<Dependency> dependencies = entry.getValue();
					Configuration detachedConfiguration = project.getConfigurations().detachedConfiguration(
							dependencies.toArray(new Dependency[dependencies.size()])
					);
					for (ResolvedDependency resolvedDependency : detachedConfiguration.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
						addTransitiveDependenciesIfNecessary(project, configuration, resolvedDependency.getChildren());
					}
				}
			}

			private void addTransitiveDependenciesIfNecessary(final Project project, final Configuration configuration, Collection<ResolvedDependency> transitiveDependencies) {
				for (final ResolvedDependency transitiveDependency : transitiveDependencies) {
					Project dependentProject = projectsByGroupAndName.get(transitiveDependency.getModuleGroup() + ":" + transitiveDependency.getModuleName());
					if (dependentProject != null) {
						Action<Project> action = new AddTransitiveProjectDependencyAction(project, configuration, transitiveDependency);
						if (!((ProjectInternal) dependentProject).getState().getExecuted()) {
							dependentProject.afterEvaluate(action);
						} else {
							action.execute(dependentProject);
						}
					} else {
						// If a corresponding project is not found locally, traverse children of external dependency
						addTransitiveDependenciesIfNecessary(project, configuration, transitiveDependency.getChildren());
					}
				}
			}
		});
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
		project.getLogger().debug("Adding dynamic dependency " + dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion() + " (" + dependency.getClass().getName() + ")");
		Dependency resolvedDependency;
		if (project.hasProperty("pride.disable")) {
			logger.info("Dynamic dependency resolution is disabled, all dynamic dependencies will be resolved to external dependencies");
			resolvedDependency = dependency;
		} else {
			resolvedDependency = localizeFirstLevelDynamicDependency(dependency);
			dynamicDependencies.put(configuration, dependency);
		}
		configuration.getDependencies().add(resolvedDependency);
	}

	private Dependency localizeFirstLevelDynamicDependency(Dependency dependency) {
		Dependency localizedDependency = dependency;
		if (dependency instanceof ExternalDependency) {
			logger.debug("Looking for " + dependency.getGroup() + ":" + dependency.getName());
			ExternalDependency externalDependency = (ExternalDependency) dependency;
			// See if we can localize this external dependency to a project dependency
			Project dependentProject = projectsByGroupAndName.get(dependency.getGroup() + ":" + dependency.getName());
			if (dependentProject != null) {
				final String targetConfiguration = externalDependency.getConfiguration();
				logger.debug("Localizing " + dependency.getGroup() + ":" + dependency.getName() + " to " + dependentProject + ", configuration: " + targetConfiguration);
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

	private static class AddTransitiveProjectDependencyAction implements Action<Project> {
		private final Project project;
		private final Configuration configuration;
		private final ResolvedDependency transitiveDependency;

		public AddTransitiveProjectDependencyAction(Project project, Configuration configuration, ResolvedDependency transitiveDependency) {
			this.project = project;
			this.configuration = configuration;
			this.transitiveDependency = transitiveDependency;
		}

		@Override
		public void execute(Project dependentProject) {
			// Sometimes we get stuff that point to non-existent configurations like "master",
			// so we should skip those
			Configuration dependentConfiguration = dependentProject.getConfigurations().findByName(transitiveDependency.getConfiguration());
			if (dependentConfiguration != null) {
				ProjectDependency projectDependency = (ProjectDependency) project.getDependencies().project(
						ImmutableMap.of(
								"path", dependentProject.getPath(),
								"configuration", dependentConfiguration.getName()
						)
				);

				// Check if we already have added this project dependency
				// either as an override, or because it's also added directly
				// as a dynamic dependency
				boolean shouldAddOverride = true;
				for (Dependency dependency : configuration.getDependencies()) {
					if (projectDependency.equals(dependency)) {
						shouldAddOverride = false;
						break;
					}
				}
				if (shouldAddOverride) {
					logger.debug("Adding override project dependency: {}", dependentConfiguration);
					// This overrides the external dependency because project
					// versions are set to Short.MAX_VALUE in generated build.gradle
					configuration.getDependencies().add(projectDependency);
				}
			}
		}
	}
}
