package com.prezi.gradle.pride;

import org.gradle.api.Project;

public class ProjectOverride {
	public final Project project;
	public final String configuration;

	public ProjectOverride(Project project, String configuration) {
		this.project = project;
		this.configuration = configuration;
	}
}
