package com.prezi.pride.cli.model;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.prezi.pride.PrideException;
import com.prezi.pride.cli.gradle.GradleConnectorManager;
import com.prezi.pride.cli.gradle.GradleProjectExecution;
import com.prezi.pride.projectmodel.PrideProjectModel;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import java.io.File;
import java.io.IOException;

public class ProjectModelAccessor {
	private final File modelInitFile;
	private final GradleConnectorManager gradleConnectorManager;
	private final boolean verbose;

	private ProjectModelAccessor(GradleConnectorManager gradleConnectorManager, boolean verbose, File modelInitFile) {
		this.gradleConnectorManager = gradleConnectorManager;
		this.verbose = verbose;
		this.modelInitFile = modelInitFile;
	}

	public static ProjectModelAccessor create(GradleConnectorManager gradleConnectorManager, boolean verbose) throws IOException {
		File modelInitFile = File.createTempFile("model-init-", ".gradle");
		Resources.asByteSource(Resources.getResource("model-init.gradle")).copyTo(Files.asByteSink(modelInitFile));
		return new ProjectModelAccessor(gradleConnectorManager, verbose, modelInitFile);
	}

	public PrideProjectModel getRootProjectModel(File moduleDirectory) {
		return gradleConnectorManager.executeInProject(moduleDirectory, new GradleProjectExecution<PrideProjectModel, RuntimeException>() {
			@Override
			public PrideProjectModel execute(File moduleDirectory, ProjectConnection connection) {
				try {
					// Load the model for the build
					ModelBuilder<PrideProjectModel> builder = connection.model(PrideProjectModel.class);
					ImmutableList.Builder<String> arguments = ImmutableList.builder();
					if (verbose) {
						arguments.add("--info", "--stacktrace");
					} else {
						arguments.add("-q");
					}

					// Add gradle-pride-projectmodel-plugin
					// See https://github.com/prezi/pride/issues/94
					arguments.add("--init-script", modelInitFile.getAbsolutePath());

					// See https://github.com/prezi/pride/issues/91
					arguments.add("--no-search-upward");

					// See https://github.com/prezi/pride/issues/57
					arguments.add("-P", "pride.disable");

					//noinspection ToArrayCallWithZeroLengthArrayArgument
					builder.withArguments(arguments.build().toArray(new String[0]));

					return builder.get();
				} catch (Exception ex) {
					throw new PrideException("Could not evaluate module in " + moduleDirectory +
							". Run './gradlew' in the pride root to get more information about the error: " + ex, ex);
				}
			}
		});
	}
}
