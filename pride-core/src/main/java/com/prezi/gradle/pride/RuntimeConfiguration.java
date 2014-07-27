package com.prezi.gradle.pride;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A configuration stack that contains defaults and overrides alongside the loaded file configurations.
 *
 * <p>The goal of this class is to maintain a stack of configurations from most-local to most-global.
 * The list of configuration is built as follows, in query-order:
 * <ul>
 *     <li>overrides</li>
 *     <li>local configuration</li>
 *     <li>...</li>
 *     <li>global configuration</li>
 *     <li>defaults</li>
 * </ul>
 */
public class RuntimeConfiguration extends AbstractConfiguration {
	public static final String PRIDE_HOME = "pride.home";
	public static final String REPO_TYPE_DEFAULT = "repo.type.default";
	public static final String REPO_BASE_URL = "repo.base.url";
	public static final String REPO_CACHE_ALWAYS = "repo.cache.always";
	public static final String REPO_RECURSIVE = "repo.recursive.always";
	public static final String COMMAND_UPDATE_REFRESH_DEPENDENCIES = "command.update.refresh_dependencies.always";
	public static final String GRADLE_VERSION = "gradle.version";
	public static final String GRADLE_HOME = "gradle.home";
	public static final String GRADLE_WRAPPER = "gradle.wrapper";

	private static class Defaults extends MapConfiguration {
		public Defaults() {
			super(new LinkedHashMap<String, Object>());
			final String property = System.getProperty("PRIDE_HOME");
			setProperty(PRIDE_HOME, property != null ? property : System.getProperty("user.home") + "/.pride");
			setProperty(REPO_TYPE_DEFAULT, "git");
			setProperty(REPO_CACHE_ALWAYS, true);
			setProperty(REPO_RECURSIVE, false);
			setProperty(COMMAND_UPDATE_REFRESH_DEPENDENCIES, false);
			setProperty(GRADLE_VERSION, null);
			setProperty(GRADLE_HOME, null);
			setProperty(GRADLE_WRAPPER, true);
		}
	}

	private final List<Configuration> configurations;

	public static RuntimeConfiguration create(Configuration... configurations) {
		ImmutableList.Builder<Configuration> builder = ImmutableList.builder();
		// Add override configuration
		builder.add(new BaseConfiguration());
		builder.addAll(Arrays.asList(configurations));
		builder.add(new Defaults());
		return new RuntimeConfiguration(builder.build());
	}

	private RuntimeConfiguration(List<Configuration> configurations) {
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
		return new RuntimeConfiguration(builder.build());
	}

	public boolean override(String property, Boolean override) {
		if (override != null) {
			setProperty(property, override);
		}
		return getBoolean(property);
	}

	public boolean override(String property, boolean overrideEnabled, boolean overrideDisabled) {
		if (overrideEnabled) {
			setProperty(property, true);
		} else if (overrideDisabled) {
			setProperty(property, false);
		}
		return getBoolean(property);
	}

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
