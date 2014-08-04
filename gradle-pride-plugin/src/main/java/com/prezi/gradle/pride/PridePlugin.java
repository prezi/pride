package com.prezi.gradle.pride;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.prezi.gradle.pride.model.PrideProjectModelBuilder;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
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
		// Check if not running from the root of a Pride
		try {
			checkIfNotRunningFromRootOfPride(project);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}

		// Register a builder for the pride tooling model
		registry.register(new PrideProjectModelBuilder());

		// Add our custom dependency declaration
		SortedSet<PrideProjectData> allProjectData;
		try {
			allProjectData = Pride.loadProjects(Pride.getPrideProjectsFile(Pride.getPrideConfigDirectory(project.getRootDir())));
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
		Map<Comparable, Object> projectsByGroupAndName = Maps.newTreeMap();
		for (PrideProjectData p : allProjectData) {
			projectsByGroupAndName.put(p.getGroup() + ":" + p.getName(), project.getRootProject().findProject(p.getPath()));
		}
		project.getExtensions().create("dynamicDependencies", DynamicDependenciesExtension.class, project, projectsByGroupAndName);

		// Apply Pride convention
		project.getConvention().getPlugins().put("pride", new PrideConvention(project));
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
