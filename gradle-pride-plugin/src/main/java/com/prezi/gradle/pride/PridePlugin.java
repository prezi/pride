package com.prezi.gradle.pride;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gradle.BuildAdapter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.invocation.Gradle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PridePlugin implements Plugin<Project> {
	private static final Logger logger = LoggerFactory.getLogger(PridePlugin.class);

	@Override
	public void apply(Project project) {
		// Check if not running from the root of a Pride
		try {
			checkIfNotRunningFromRootOfPride(project);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Add our custom dependency declaration
		DynamicDependenciesExtension extension = project.getExtensions().create("dynamicDependencies", DynamicDependenciesExtension.class, project);

		// Apply Pride convention
		project.getConvention().getPlugins().put("pride", new PrideConvention(project));

		localizeDynamicDependencies(project, extension);
	}

	private static void localizeDynamicDependencies(final Project project, final DynamicDependenciesExtension dynamicDependencies) {
		project.getGradle().addBuildListener(new BuildAdapter() {
			@Override
			public void projectsEvaluated(Gradle gradle) {
				final Map<String, Project> projectPathsByGroupAndName = Maps.uniqueIndex(project.getRootProject().getAllprojects(), new Function<Project, String>() {
					@Override
					public String apply(Project p) {
						return p.getGroup() + ":" + p.getName();
					}
				});

				// Collect local projects in the session
				logger.debug("Resolving dynamic dependencies among projects: " + projectPathsByGroupAndName.keySet());
				for (Map.Entry<String, List<Dependency>> entry : dynamicDependencies.getDependencies().entrySet()) {
					Configuration configuration = project.getConfigurations().getByName(entry.getKey());
					List<Dependency> dependencies = entry.getValue();
					Set<Dependency> localizedDependencies = localizeDynamicDependencies(dependencies, project, projectPathsByGroupAndName);
					configuration.getDependencies().addAll(localizedDependencies);
				}
			}
		});
	}

	private static Set<Dependency> localizeDynamicDependencies(Collection<Dependency> dependencies, final Project project, final Map<String, Project> projectsByGroupAndName) {
		// Localize first-level dependencies first
		final LocalProjectResolver localProjectResolver = new DefaultLocalProjectResolver(project);
		final Map<String, String> projectPathsByGroupAndName = Maps.transformEntries(projectsByGroupAndName, new Maps.EntryTransformer<String, Project, String>() {
			@Override
			public String transformEntry(String id, Project p) {
				return p.getPath();
			}
		});

		Set<Dependency> localizedDependencies = Sets.newLinkedHashSet();
		for (Dependency dependency : dependencies) {
			localizedDependencies.add(localizeFirstLevelDynamicDependency(dependency, projectPathsByGroupAndName, localProjectResolver));
		}

		// Go through transitive dependencies and replace them with projects when applicable
		// See https://github.com/prezi/pride/issues/40
		Configuration detachedConfiguration = project.getConfigurations().detachedConfiguration();
		final Set<ProjectOverride> projectOverrides = Sets.newLinkedHashSet();
		for (ResolvedDependency resolvedDependency : detachedConfiguration.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
			collectTransitiveDependenciesToOverride(projectsByGroupAndName, resolvedDependency.getChildren(), projectOverrides);
		}
		for (ProjectOverride projectOverride : projectOverrides) {
			logger.debug("Adding override project dependency: {}", projectOverride);
			// This overrides the external dependency because project
			// versions are set to Short.MAX_VALUE in generated build.gradle
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(2);
			map.put("path", projectOverride.project.getPath());
			map.put("configuration", projectOverride.configuration);
			localizedDependencies.add(project.getDependencies().project(map));
		}
		return localizedDependencies;
	}

	public static Dependency localizeFirstLevelDynamicDependency(final Dependency dependency, Map<String, String> projectPathsByGroupAndName, LocalProjectResolver projectResolver) {
		Dependency localizedDependency = dependency;
		if (dependency instanceof ExternalDependency) {
			logger.debug("Looking for " + dependency.getGroup() + ":" + dependency.getName());
			ExternalDependency externalDependency = (ExternalDependency) dependency;
			// See if we can localize this external dependency to a project dependency
			final String dependentProjectPath = projectPathsByGroupAndName.get(dependency.getGroup() + ":" + dependency.getName());
			if (!StringUtils.isEmpty(dependentProjectPath)) {
				final String targetConfiguration = externalDependency.getConfiguration();
				logger.debug("Localizing " + dependency.getGroup() + ":" + dependency.getName() + " to " + dependentProjectPath + ", configuration: " + targetConfiguration);
				ProjectDependency projectDependency = projectResolver.resolveLocalProject(dependentProjectPath, targetConfiguration);
				projectDependency.setTransitive(externalDependency.isTransitive());
				projectDependency.getExcludeRules().addAll(externalDependency.getExcludeRules());
				projectDependency.getArtifacts().addAll(externalDependency.getArtifacts());
				localizedDependency = projectDependency;
			}
		}
		return localizedDependency;
	}

	private static void collectTransitiveDependenciesToOverride(Map<String, Project> projectsByGroupAndName, Set<ResolvedDependency> dependencies, Set<ProjectOverride> projectOverrides) {
		for (ResolvedDependency dependency : dependencies) {
			Project dependentProject = projectsByGroupAndName.get(dependency.getModuleGroup() + ":" + dependency.getModuleName());
			if (dependentProject != null) {
				// Sometimes we get stuff that point to non-existent configurations like "master",
				// so we should skip those
				if (dependentProject.getConfigurations().findByName(dependency.getConfiguration()) != null) {
					projectOverrides.add(new ProjectOverride(dependentProject, dependency.getConfiguration()));
				}
			} else {
				// If a corresponding project is not found locally, traverse children of external dependency
				collectTransitiveDependenciesToOverride(projectsByGroupAndName, dependency.getChildren(), projectOverrides);
			}
		}
	}

	private static boolean alreadyCheckedIfRunningFromRootOfPride;

	private static void checkIfNotRunningFromRootOfPride(final Project project) throws IOException {
		if (!alreadyCheckedIfRunningFromRootOfPride) {
			if (!Pride.containsPride(project.getRootDir())) {
				logger.debug("No pride found in " + String.valueOf(project.getRootDir()));
				for (File dir = project.getRootDir().getParentFile(); dir != null && dir.canRead(); dir = dir.getParentFile()) {
					logger.debug("Checking pride in " + dir);
					if (Pride.containsPride(dir)) {
						logger.warn("WARNING: Found a pride in parent directory " + dir + ". " +
								"This means that you are running Gradle from a subproject of the pride. " +
								"Dynamic dependencies cannot be resolved to local projects this way. " +
								"To avoid this warning run Gradle from the root of the pride.");
						break;
					}
				}
			}

			alreadyCheckedIfRunningFromRootOfPride = true;
		}

	}
}

