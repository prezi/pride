package com.prezi.pride.projectmodel;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.reflect.MethodUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DynamicDependenciesExtractor {
	public static Map<String, Set<DynamicDependency>> getDynamicDependencies(Project project) {
		try {
			Map<String, Set<DynamicDependency>> dynamicDependencies = Maps.newLinkedHashMap();
			Object extension = project.getExtensions().findByName("dynamicDependencies");
			if (extension != null) {
				@SuppressWarnings("unchecked")
				Map<String, Collection<Dependency>> requestedDynamicDependencies = (Map<String, Collection<Dependency>>) MethodUtils.invokeExactMethod(extension, "getRequestedDynamicDependencies", new Object[0]);
				for (Map.Entry<String, Collection<Dependency>> entry : requestedDynamicDependencies.entrySet()) {
					String configuration = entry.getKey();
					Set<DynamicDependency> dependencies = Sets.newLinkedHashSet();
					for (Dependency dependency : entry.getValue()) {
						dependencies.add(new DefaultDynamicDependency(dependency.getGroup(), dependency.getName(), dependency.getVersion()));
					}
					dynamicDependencies.put(configuration, dependencies);
				}
			}
			return dynamicDependencies;
		} catch (Exception e) {
			project.getLogger().warn("Could not get dynamic dependencies", e);
			return Collections.emptyMap();
		}
	}
}
