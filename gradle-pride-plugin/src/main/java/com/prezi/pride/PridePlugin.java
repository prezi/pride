package com.prezi.pride;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails2;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;

public class PridePlugin implements Plugin<Project> {
	private static final Logger logger = LoggerFactory.getLogger(PridePlugin.class);

	// All local projects have their version set to a high value in the generated build.gradle
	// in order to override external dependencies
	public static final String LOCAL_PROJECT_VERSION = String.valueOf(Short.MAX_VALUE);

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

		// Add our custom dependency declaration
		final Map<String, Project> projectsByGroupAndName = Maps.newTreeMap();
		if (!prideDisabled && Pride.containsPride(project.getRootDir())) {
			SortedSet<PrideProjectData> allProjectData;
			try {
				allProjectData = Pride.loadProjects(Pride.getPrideProjectsFile(Pride.getPrideConfigDirectory(project.getRootDir())));
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
			for (PrideProjectData p : allProjectData) {
				projectsByGroupAndName.put(p.getGroup() + ":" + p.getName(), project.getRootProject().project(p.getPath()));
			}
		}

		// Apply Pride convention
		project.getConvention().getPlugins().put("pride", new PrideConvention(project));

		// Use replacement rule
		if (!isDisabled(project)) {
			project.getConfigurations().all(new Action<Configuration>() {
				@Override
				public void execute(Configuration configuration) {
					configuration.getResolutionStrategy().eachDependency2(new Action<DependencyResolveDetails2>() {
						@Override
						public void execute(DependencyResolveDetails2 details) {
							if (!(details.getRequested() instanceof ModuleComponentSelector)) {
								logger.warn("We have been called with a project! {}", details.getRequested());
								return;
							}
							ModuleComponentSelector requested = (ModuleComponentSelector) details.getRequested();
							String id = requested.getGroup() + ":" + requested.getModule();
							Project dependentProject = projectsByGroupAndName.get(id);
							if (dependentProject != null) {
								logger.info("Replaced external dependency {} with {}", requested, dependentProject);
								details.useTarget(dependentProject);
							}
						}
					});
				}
			});
		}

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
}
