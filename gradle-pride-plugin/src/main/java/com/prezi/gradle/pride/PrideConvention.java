package com.prezi.gradle.pride;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.UnknownProjectException;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.util.ConfigureUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds a few useful methods to look up relative projects via project paths like ":sibling" or "::aunt".
 * We need this because Gradle by default does not allow relative project references to parent projects,
 * only children.
 *
 * Created by lptr on 02/04/14.
 */
public class PrideConvention {

	private final Project project;

	public PrideConvention(final Project project) {
		this.project = project;
	}

	public static Map<String, ?> resolveProjectPath(Project project, Map<String, ?> notation) {
		if (!notation.containsKey("path")) {
			return notation;
		}

		String path = (String) notation.get("path");
		Map<String, Object> absoluteNotation = new LinkedHashMap<String, Object>(notation);
		Project resolvedProject = findRelativeProjectInternal(project, path);
		if (!DefaultGroovyMethods.asBoolean(resolvedProject)) {
			throw new UnknownProjectException("Could not find relative project at path \"" + path + "\"");
		}
		absoluteNotation.put("path", resolvedProject.getPath());
		return absoluteNotation;
	}

	private static Project findRelativeProjectInternal(Project parent, String path) {
		if (path.isEmpty()) {
			return parent;
		}

		if (path.startsWith(":")) {
			return findRelativeProjectInternal(parent.getParent(), path.substring(1));
		}

		return parent.findProject(path);
	}

	/**
	 * <p>Locates a project by path. If the path is relative, it is interpreted relative to this project.</p>
	 *
	 * @param path The path.
	 * @return The project with the given path. Returns null if no such project exists.
	 */
	public Project findRelativeProject(String path) {
		return findRelativeProjectInternal(project, path);
	}

	/**
	 * <p>Locates a project by path. If the path is relative, it is interpreted relative to this project.</p>
	 *
	 * @param path The path.
	 * @return The project with the given path. Never returns null.
	 * @throws org.gradle.api.UnknownProjectException If no project with the given path exists.
	 */
	public Project relativeProject(String path) throws UnknownProjectException {
		Project project = findRelativeProject(path);
		if (project == null) {
			throw new UnknownProjectException(String.format("Project with path '%s' could not be found in %s.", path, this));
		}

		return project;
	}

	/**
	 * <p>Locates a project dependency by notation. If the path is relative, it is interpreted relative to this project.
	 * This is to be used from the {@code dependencies { ... }} block.</p>
	 *
	 * @param notation A map containing the parameters to create the projet.
	 * @return The project with the given path. Never returns null.
	 * @throws org.gradle.api.UnknownProjectException If no project with the given path exists.
	 */
	public ProjectDependency relativeProject(Map<String, ?> notation) {
		return (ProjectDependency) project.getDependencies().project(resolveProjectPath(project, notation));
	}

	/**
	 * <p>Locates a project by path and configures it using the given closure. If the path is relative, it is
	 * interpreted relative to this project. The target project is passed to the closure as the closure's delegate.</p>
	 *
	 * @param path             The path.
	 * @param configureClosure The closure to use to configure the project.
	 * @return The project with the given path. Never returns null.
	 * @throws org.gradle.api.UnknownProjectException If no project with the given path exists.
	 */
	public Project relativeProject(String path, Closure configureClosure) throws UnknownProjectException {
		return ConfigureUtil.configure(configureClosure, relativeProject(path));
	}
}
