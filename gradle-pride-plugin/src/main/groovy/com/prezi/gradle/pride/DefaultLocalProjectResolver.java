package com.prezi.gradle.pride;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;

import java.util.LinkedHashMap;

public class DefaultLocalProjectResolver implements LocalProjectResolver {
	public DefaultLocalProjectResolver(Project project) {
		this.project = project;
	}

	@Override
	public ProjectDependency resolveLocalProject(String path, String configuration) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(2);
		map.put("path", path);
		map.put("configuration", configuration);
		return (ProjectDependency) project.getDependencies().project(map);
	}

	private final Project project;
}
