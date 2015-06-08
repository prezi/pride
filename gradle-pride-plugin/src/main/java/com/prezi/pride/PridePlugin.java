package com.prezi.pride;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySubstitution;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class PridePlugin implements Plugin<Project> {
	private static final Logger logger = LoggerFactory.getLogger(PridePlugin.class);

	@Override
	public void apply(Project project) {
		boolean prideDisabled = isDisabled(project);
		if (!prideDisabled) {
			// Check if not running from the root of a Pride
			try {
				checkIfNotRunningFromRootOfPride(project);
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
		}

		// Apply Pride convention
		project.getConvention().getPlugins().put("pride", new PrideConvention(project));

		// Add dynamicDependencies extension for backwards compatibility
		//noinspection deprecation
		project.getExtensions().create("dynamicDependencies", DynamicDependenciesExtension.class, project);

		// Use replacement rule
		if (!isDisabled(project)) {
			final Set<Project> projects = project.getRootProject().getAllprojects();
			project.getConfigurations().all(new Action<Configuration>() {
				@Override
				public void execute(Configuration configuration) {
					configuration.getResolutionStrategy().getDependencySubstitution().all(new ReplaceDependenciesAction(projects));
				}
			});
		}

		// Check dependency versions
		CheckDependencyVersionsTask checkVersions = project.getTasks().create("checkDependencyVersions", CheckDependencyVersionsTask.class);
		checkVersions.setConfigurations(project.getConfigurations());
		checkVersions.setDescription("Checks if substituted dependency projects conform to their requested versions");

		// See https://github.com/prezi/pride/issues/100
		project.afterEvaluate(new Action<Project>() {
			@Override
			public void execute(Project project) {
				if (project.getGroup() == null || String.valueOf(project.getGroup()).isEmpty()) {
					throw new IllegalStateException("Group is not specified for project in " + project.getProjectDir());
				}
			}
		});
	}

	private static boolean alreadyCheckedIfRunningFromRootOfPride;

	private static void checkIfNotRunningFromRootOfPride(final Project project) throws IOException {
		// Don't check for a pride when not searching upward
		if (!project.getGradle().getStartParameter().isSearchUpwards()) {
			return;
		}
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

	public static boolean isDisabled(Project project) {
		return project.hasProperty("pride.disable");
	}

	private static class ReplaceDependenciesAction implements Action<DependencySubstitution> {
		private Map<String, Project> modulesToProjectsMapping;
		private final Set<Project> projects;

		public ReplaceDependenciesAction(Set<Project> projects) {
			this.projects = projects;
		}

		@Override
		public void execute(DependencySubstitution details) {
			// Skip components that are not external components
			if (!(details.getRequested() instanceof ModuleComponentSelector)) {
				return;
			}
			ModuleComponentSelector selector = (ModuleComponentSelector) details.getRequested();
			if (modulesToProjectsMapping == null) {
				modulesToProjectsMapping = Maps.newTreeMap();
				for (Project project : projects) {
					modulesToProjectsMapping.put(project.getGroup() + ":" + project.getName(), project);
				}
				logger.info("Modules to projects mapping: {}", modulesToProjectsMapping);
			}
			String id = selector.getGroup() + ":" + selector.getModule();
			Project dependentProject = modulesToProjectsMapping.get(id);
			if (dependentProject != null) {
				logger.info("Replaced external dependency {} with {}", selector, dependentProject);
				details.useTarget(dependentProject);
			}
		}
	}
}
