package com.prezi.gradle.pride;

import org.gradle.api.artifacts.ProjectDependency;

public interface LocalProjectResolver {
	public abstract ProjectDependency resolveLocalProject(String path, String configuration);
}
