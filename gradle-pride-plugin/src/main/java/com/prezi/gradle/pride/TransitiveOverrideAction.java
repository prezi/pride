package com.prezi.gradle.pride;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.internal.project.ProjectInternal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

public class TransitiveOverrideAction implements Action<Project> {
	private static final Logger logger = LoggerFactory.getLogger(TransitiveOverrideAction.class);

	private final Map<String, Project> projectsByGroupAndName;

	public TransitiveOverrideAction(Map<String, Project> projectsByGroupAndName) {
		this.projectsByGroupAndName = projectsByGroupAndName;
	}

	@Override
	public void execute(Project project) {
		if (!project.getPlugins().hasPlugin(PridePlugin.class)) {
			return;
		}
		DynamicDependenciesExtension extension = project.getExtensions().getByType(DynamicDependenciesExtension.class);
		SetMultimap<String, Dependency> dynamicDependencies = extension.getDynamicDependencies();
		for (Map.Entry<String, Collection<Dependency>> entry : dynamicDependencies.asMap().entrySet()) {
			Configuration configuration = project.getConfigurations().getByName(entry.getKey());
			Collection<Dependency> externalDependencies = Collections2.filter(entry.getValue(), new Predicate<Dependency>() {
				@Override
				public boolean apply(Dependency dependency) {
					return !dependency.getVersion().equals(String.valueOf(Short.MAX_VALUE));
				}
			});
			Configuration detachedConfiguration = project.getConfigurations().detachedConfiguration(
					externalDependencies.toArray(new Dependency[externalDependencies.size()])
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
				// afterEvaluate does not complete if the project is already evaluated
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
