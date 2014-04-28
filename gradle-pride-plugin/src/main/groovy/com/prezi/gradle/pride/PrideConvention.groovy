package com.prezi.gradle.pride

import org.gradle.api.Project
import org.gradle.api.UnknownProjectException
import org.gradle.util.ConfigureUtil

/**
 * Adds a few useful methods to look up relative projects via project paths like ":sibling" or "::aunt".
 * We need this because Gradle by default does not allow relative project references to parent projects,
 * only children.
 *
 * Created by lptr on 02/04/14.
 */
class PrideConvention {
	Project project

	PrideConvention(Project project) {
		this.project = project

		// Add relativeProject() method to dependencies { ... } block
		project.dependencies.metaClass.relativeProject = { Map<String, ?> notation ->
			return project.dependencies.project(resolveProjectPath(project, notation))
		}
	}

	public static Map<String, ?> resolveProjectPath(Project project, Map<String, ?> notation) {
		Map<String, ?> absoluteNotation = notation
		if (notation.containsKey("path")) {
			String path = notation.get("path")
			absoluteNotation = new LinkedHashMap<>(notation)
			Project resolvedProject = findRelativeProjectInternal(project, path)
			if (!resolvedProject) {
				throw new UnknownProjectException("Could not find relative project at path \"${path}\"")
			}
			absoluteNotation.put("path", resolvedProject.path)
		}
		return absoluteNotation
	}

	private static Project findRelativeProjectInternal(Project parent, String path) {
		if (path.empty) {
			return parent
		}
		if (path.startsWith(":")) {
			return findRelativeProjectInternal(parent.parent, path.substring(1))
		}
		return parent.findProject(path)
	}

	/**
	 * <p>Locates a project by path. If the path is relative, it is interpreted relative to this project.</p>
	 *
	 * @param path The path.
	 * @return The project with the given path. Returns null if no such project exists.
	 */
	Project findRelativeProject(String path) {
		return findRelativeProjectInternal(project, path)
	}

	/**
	 * <p>Locates a project by path. If the path is relative, it is interpreted relative to this project.</p>
	 *
	 * @param path The path.
	 * @return The project with the given path. Never returns null.
	 * @throws org.gradle.api.UnknownProjectException If no project with the given path exists.
	 */
	Project relativeProject(String path) throws UnknownProjectException {
		Project project = findRelativeProject(path)
		if (project == null) {
			throw new UnknownProjectException(String.format("Project with path '%s' could not be found in %s.", path, this));
		}
		return project;
	}

	/**
	 * <p>Locates a project by path and configures it using the given closure. If the path is relative, it is
	 * interpreted relative to this project. The target project is passed to the closure as the closure's delegate.</p>
	 *
	 * @param path The path.
	 * @param configureClosure The closure to use to configure the project.
	 * @return The project with the given path. Never returns null.
	 * @throws UnknownProjectException If no project with the given path exists.
	 */
	Project relativeProject(String path, Closure configureClosure) throws UnknownProjectException {
		return ConfigureUtil.configure(configureClosure, relativeProject(path))
	}
}
