package com.prezi.pride;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.prezi.pride.ivyversions.ResolverStrategy;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class CheckDependencyVersionsTask extends DefaultTask {

	private Set<Configuration> configurations = Sets.newLinkedHashSet();

	private final ProjectFinder projectFinder;

	@Inject
	public CheckDependencyVersionsTask(ProjectFinder projectFinder) {
		this.projectFinder = projectFinder;
	}

	@TaskAction
	public void checkVersions() {
		final Set<String> versionConflicts = Sets.newLinkedHashSet();
		for (final Configuration configuration : configurations) {
			configuration.getIncoming().getResolutionResult().allDependencies(new Action<DependencyResult>() {
				@Override
				public void execute(DependencyResult result) {
					if (result instanceof ResolvedDependencyResult) {
						ResolvedDependencyResult resolvedResult = (ResolvedDependencyResult) result;
						ComponentSelector requested = resolvedResult.getRequested();
						if (requested instanceof ModuleComponentSelector) {
							ModuleComponentSelector requestedSelector = (ModuleComponentSelector) requested;
							String requestedVersion = requestedSelector.getVersion();
							ComponentIdentifier selected = resolvedResult.getSelected().getId();
							if (selected instanceof ProjectComponentIdentifier) {
								ProjectComponentIdentifier selectedProjectComponent = (ProjectComponentIdentifier) selected;
								ProjectInternal selectedProject = projectFinder.getProject(selectedProjectComponent.getProjectPath());
								Object selectedVersionObject = selectedProject.getVersion();
								if (selectedVersionObject != null) {
									String selectedVersion = selectedVersionObject.toString();
									if (!matchVersion(requestedVersion, selectedVersion)) {
										String conflict = String.format("Configuration \"%s\" in project \"%s\" requests version %s of project \"%s\", but its current version (%s) does not fulfill that request", configuration.getName(), getProject().getPath(), requestedVersion, selectedProject.getPath(), selectedVersion);
										versionConflicts.add(conflict);
									}
								}
							}
						}
					}
				}
			});
		}
		if (!versionConflicts.isEmpty()) {
			for (String conflict : versionConflicts) {
				getLogger().warn(conflict);
			}
		} else {
			getLogger().info("All projects refer to dependency versions that will resolve to the ones in the pride");
		}
	}

	@Input
	public Set<Configuration> getConfigurations() {
		return configurations;
	}

	public void setConfigurations(Set<Configuration> configurations) {
		this.configurations = configurations;
	}

	public void setConfigurations(Configuration... configurations) {
		this.configurations = Sets.newLinkedHashSet(Arrays.asList(configurations));
	}

	public void configurations(Configuration... configurations) {
		Collections.addAll(this.configurations, configurations);
	}

	public void configurations(Iterable<Configuration> configurations) {
		Iterables.addAll(this.configurations, configurations);
	}

	@Option(option = "configuration", description = "The configuration to generate the report for.")
	public void setConfiguration(String configurationName) {
		this.configurations = Collections.singleton(getProject().getConfigurations().getByName(configurationName));
	}

	static boolean matchVersion(String requested, String actual) {
		return new ResolverStrategy().accept(requested, actual);
	}
}
