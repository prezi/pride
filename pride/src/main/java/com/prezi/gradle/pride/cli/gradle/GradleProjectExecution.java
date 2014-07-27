package com.prezi.gradle.pride.cli.gradle;

import org.gradle.tooling.ProjectConnection;

import java.io.File;

public interface GradleProjectExecution<T, E extends Exception> {
	T execute(File projectDirectory, ProjectConnection connection) throws E;
}
