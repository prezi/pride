package com.prezi.gradle.pride

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by lptr on 11/04/14.
 */
class PridePlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		// Add our custom dependency declaration
		project.extensions.create("dynamicDependencies", DynamicDependenciesExtension, project)

		// Apply Pride convention
		project.convention.plugins.pride = new PrideConvention(project)
	}
}
