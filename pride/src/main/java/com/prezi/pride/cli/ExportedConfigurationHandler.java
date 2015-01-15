package com.prezi.pride.cli;

import com.prezi.pride.config.AbstractConfigurationHandler;
import com.prezi.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;

public class ExportedConfigurationHandler extends AbstractConfigurationHandler<ExportedModule> {
	public ExportedConfigurationHandler(VcsManager vcsManager) {
		super(vcsManager);
	}

	@Override
	protected ExportedModule loadModule(Configuration config, String prefix) {
		String moduleName = config.getString(prefix + ".name");
		String moduleRemote = config.getString(prefix + ".remote");
		String moduleRevision = config.getString(prefix + ".revision");
		String vcsType = config.getString(prefix + ".vcs");
		return new ExportedModule(moduleName, moduleRemote, moduleRevision, getVcsManager().getVcs(vcsType, config));
	}

	@Override
	protected void saveModule(Configuration config, String prefix, ExportedModule module) {
		config.setProperty(prefix + ".name", module.getName());
		config.setProperty(prefix + ".remote", module.getRemote());
		config.setProperty(prefix + ".revision", module.getRevision());
		config.setProperty(prefix + ".vcs", module.getVcs().getType());
	}
}
