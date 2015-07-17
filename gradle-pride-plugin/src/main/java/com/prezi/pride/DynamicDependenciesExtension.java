package com.prezi.pride;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class DynamicDependenciesExtension extends GroovyObjectSupport {
	@SuppressWarnings("deprecation")
	private static final Logger logger = LoggerFactory.getLogger(DynamicDependenciesExtension.class);
	private final DefaultDependencyHandler dependencyHandler;
	private static boolean userNagged;

	public DynamicDependenciesExtension(Project project) {
		this.dependencyHandler = (DefaultDependencyHandler) project.getDependencies();
	}

	@SuppressWarnings("UnusedDeclaration")
	public Object methodMissing(String name, Object args) {
		nagUser();
		return dependencyHandler.methodMissing(name, args);
	}

	public Dependency add(String s, Object o) {
		nagUser();
		return dependencyHandler.add(s, o);
	}

	public Dependency add(String s, Object o, Closure<?> closure) {
		nagUser();
		return dependencyHandler.add(s, o, closure);
	}

	private void nagUser() {
		if (!userNagged) {
			userNagged = true;
			logger.warn("Using dynamicDependencies is deprecated, please use the regular dependencies block to define dependencies instead. "
					+ "The dynamicDependencies extension will be removed in Pride 1.0.");
		}
	}
}
