package com.prezi.pride.config;

import com.prezi.pride.Module;
import com.prezi.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;

public class PrideConfigurationHandler extends AbstractConfigurationHandler<Module> {
	public PrideConfigurationHandler(VcsManager vcsManager) {
		super(vcsManager);
	}

	@Override
	protected Module loadModule(Configuration config, String prefix) {
		String moduleName = config.getString(prefix + ".name");
		String vcsType = config.getString(prefix + ".vcs");
		return new Module(moduleName, getVcsManager().getVcs(vcsType, config));
	}

	@Override
	protected void saveModule(Configuration config, String prefix, Module module) {
		config.setProperty(prefix + ".name", module.getName());
		config.setProperty(prefix + ".vcs", module.getVcs().getType());
	}
}
