package com.prezi.gradle.pride.projectmodel;

import java.io.Serializable;

public class DefaultDynamicDependency implements DynamicDependency, Serializable {
	private final String group;
	private final String name;
	private final String version;

	public DefaultDynamicDependency(String group, String name, String version) {
		this.group = group;
		this.name = name;
		this.version = version;
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return "DefaultDynamicDependency{" +
				"group='" + group + '\'' +
				", name='" + name + '\'' +
				", version='" + version + '\'' +
				'}';
	}
}
