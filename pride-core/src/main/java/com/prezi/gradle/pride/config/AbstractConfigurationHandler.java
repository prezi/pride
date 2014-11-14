package com.prezi.gradle.pride.config;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.vcs.VcsManager;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractConfigurationHandler<M> {

	private static final String MODULES_KEY = "modules";
	private static final Pattern MODULE_ID_MATCHER = Pattern.compile("modules\\.(\\d+)\\..*");
	private final VcsManager vcsManager;

	protected AbstractConfigurationHandler(VcsManager vcsManager) {
		this.vcsManager = vcsManager;
	}

	public ConfigurationData<M> loadConfiguration(Configuration fileConfiguration) {
		BaseConfiguration configuration = new BaseConfiguration();
		configuration.copy(fileConfiguration);

		// Remove module data from config
		Iterator<String> moduleKeys = configuration.getKeys("modules.");
		while (moduleKeys.hasNext()) {
			configuration.clearProperty(moduleKeys.next());
		}

		List<M> modules = Lists.newArrayList();
		Set<String> moduleIds = Sets.newLinkedHashSet();
		for (String moduleKey : Iterators.toArray(fileConfiguration.getKeys(MODULES_KEY), String.class)) {
			Matcher matcher = MODULE_ID_MATCHER.matcher(moduleKey);
			if (!matcher.matches()) {
				throw new PrideException("Invalid module setting: " + moduleKey);
			}
			String moduleId = matcher.group(1);
			moduleIds.add(moduleId);
		}
		for (String moduleId : moduleIds) {
			String prefix = MODULES_KEY + "." + moduleId;
			M module = loadModule(fileConfiguration, prefix);
			modules.add(module);
		}
		return new ConfigurationData<M>(configuration, modules);
	}

	public void saveConfiguration(Configuration configuration, Collection<M> modules) {
		for (String moduleKey : Iterators.toArray(configuration.getKeys(MODULES_KEY), String.class)) {
			configuration.clearProperty(moduleKey);
		}
		int id = 0;
		for (M module : modules) {
			String moduleId = MODULES_KEY + "." + id;
			saveModule(configuration, moduleId, module);
			id++;
		}
	}

	abstract protected M loadModule(Configuration config, String prefix);

	abstract protected void saveModule(Configuration config, String prefix, M module);

	protected VcsManager getVcsManager() {
		return vcsManager;
	}
}
