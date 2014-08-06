package com.prezi.gradle.pride;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.prezi.gradle.pride.model.PrideProjectModelBuilder;
import groovy.lang.Closure;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;

public class PridePlugin implements Plugin<Project> {
	private static final Logger logger = LoggerFactory.getLogger(PridePlugin.class);

	private final ToolingModelBuilderRegistry registry;

	@Inject
	public PridePlugin(ToolingModelBuilderRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void apply(Project project) {
		boolean prideDisabled = project.hasProperty("pride.disable");
		if (!prideDisabled) {
			// Check if not running from the root of a Pride
			try {
				checkIfNotRunningFromRootOfPride(project);
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
		}

		// Register a builder for the pride tooling model
		registry.register(new PrideProjectModelBuilder());

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
		project.getExtensions().create("dynamicDependencies", DynamicDependenciesExtension.class, project, projectsByGroupAndName);

		// Apply Pride convention
		project.getConvention().getPlugins().put("pride", new PrideConvention(project));

		// Go through transitive dependencies and replace them with projects when applicable.
		// See https://github.com/prezi/pride/issues/40
		// See https://github.com/prezi/pride/issues/87
		// Do this in the root project
		if (!prideDisabled) {
			if (!project.getRootProject().hasProperty("pride.init")) {
				project.getRootProject().getConvention().getExtraProperties().set("pride.init", true);
				final Gradle gradle = project.getGradle();
				gradle.projectsEvaluated(new Closure(gradle) {
					@SuppressWarnings("UnusedDeclaration")
					public void doCall(Object args) {
						gradle.allprojects(new TransitiveOverrideAction(projectsByGroupAndName));
					}
				});
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
