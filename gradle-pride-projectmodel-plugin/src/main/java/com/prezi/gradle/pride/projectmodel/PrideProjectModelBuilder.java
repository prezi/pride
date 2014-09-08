package com.prezi.gradle.pride.projectmodel;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

public class PrideProjectModelBuilder implements ToolingModelBuilder {
	@Override
	public boolean canBuild(String modelName) {
		return modelName.equals(PrideProjectModel.class.getName());
	}

	@Override
	public Object buildAll(String s, Project project) {
		return convertProject(project.getRootProject());
	}

	private PrideProjectModel convertProject(Project project) {
		ImmutableSet.Builder<PrideProjectModel> childModels = ImmutableSet.builder();
		for (Project childProject : project.getChildProjects().values()) {
			childModels.add(convertProject(childProject));
		}
		if (project.getGroup() == null || String.valueOf(project.getGroup()).isEmpty()) {
			throw new IllegalStateException("Group is not specified for project in " + project.getProjectDir());
		}
		return new DefaultPrideProjectModel(
				project.getPath(),
				String.valueOf(project.getGroup()),
				project.getName(),
				childModels.build(),
				project.getProjectDir().getAbsolutePath()
		);
	}
}
