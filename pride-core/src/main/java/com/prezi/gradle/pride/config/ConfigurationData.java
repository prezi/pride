package com.prezi.gradle.pride.config;

import org.apache.commons.configuration.Configuration;

import java.util.Collection;

public class ConfigurationData<T> {
	private final Configuration configuration;
	private final Collection<T> modules;

	public ConfigurationData(Configuration configuration, Collection<T> modules) {
		this.configuration = configuration;
		this.modules = modules;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public Collection<T> getModules() {
		return modules;
	}
}
