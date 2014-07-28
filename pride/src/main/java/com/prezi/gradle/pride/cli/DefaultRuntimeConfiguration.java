package com.prezi.gradle.pride.cli;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.prezi.gradle.pride.RuntimeConfiguration;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DefaultRuntimeConfiguration extends AbstractConfiguration implements RuntimeConfiguration {
	private final List<Configuration> configurations;

	public static RuntimeConfiguration create(Configuration... configurations) {
		ImmutableList.Builder<Configuration> builder = ImmutableList.builder();
		// Add override configuration
		builder.add(new BaseConfiguration());
		builder.addAll(Arrays.asList(configurations));
		builder.add(new Configurations.Defaults());
		return new DefaultRuntimeConfiguration(builder.build());
	}

	private DefaultRuntimeConfiguration(List<Configuration> configurations) {
		this.configurations = configurations;
	}

	public RuntimeConfiguration withConfiguration(Configuration config) {
		ImmutableList.Builder<Configuration> builder = ImmutableList.builder();
		// Add the overrides configuration
		builder.add(configurations.get(0));
		// Add the new configuration
		builder.add(config);
		// Add all the rest of the old configurations
		builder.addAll(configurations.subList(1, configurations.size()));
		return new DefaultRuntimeConfiguration(builder.build());
	}

	@Override
	public boolean override(String property, Boolean override) {
		if (override != null) {
			setProperty(property, override);
		}
		return getBoolean(property);
	}

	@Override
	public boolean override(String property, boolean overrideEnabled, boolean overrideDisabled) {
		if (overrideEnabled) {
			setProperty(property, true);
		} else if (overrideDisabled) {
			setProperty(property, false);
		}
		return getBoolean(property);
	}

	@Override
	public String override(String property, String override) {
		if (!Strings.isNullOrEmpty(override)) {
			setProperty(property, override);
		}
		return getString(property);
	}

	@Override
	protected void addPropertyDirect(String key, Object value) {
		configurations.get(0).setProperty(key, value);
	}

	@Override
	public boolean isEmpty() {
		for (Configuration config : configurations) {
			if (!config.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean containsKey(String key) {
		for (Configuration config : configurations) {
			if (config.containsKey(key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object getProperty(String key) {
		for (Configuration config : configurations) {
			if (config.containsKey(key)) {
				return config.getProperty(key);
			}
		}
		return null;
	}

	@Override
	public Iterator<String> getKeys() {
		return Iterators.concat(Iterables.transform(configurations, new Function<Configuration, Iterator<String>>() {
			@Override
			public Iterator<String> apply(Configuration config) {
				return config.getKeys();
			}
		}).iterator());
	}
}
