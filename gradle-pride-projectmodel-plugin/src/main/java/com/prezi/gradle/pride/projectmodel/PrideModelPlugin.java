package com.prezi.gradle.pride.projectmodel;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import javax.inject.Inject;

@SuppressWarnings("UnusedDeclaration")
public class PrideModelPlugin implements Plugin<Project> {
	private final ToolingModelBuilderRegistry registry;

	@SuppressWarnings("UnusedDeclaration")
	@Inject
	public PrideModelPlugin(ToolingModelBuilderRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void apply(Project project) {
		// Register a builder for the pride tooling model
		registry.register(new PrideProjectModelBuilder());
	}
}
