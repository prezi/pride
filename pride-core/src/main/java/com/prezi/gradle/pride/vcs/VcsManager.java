package com.prezi.gradle.pride.vcs;

import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.vcs.file.FileVcsSupportFactory;
import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public final class VcsManager {

	private final Map<String, VcsSupportFactory> vcss = new LinkedHashMap<String, VcsSupportFactory>();

	public VcsManager() {
		for (VcsSupportFactory factory : ServiceLoader.load(VcsSupportFactory.class)) {
			vcss.put(factory.getType(), factory);
		}
		// Make sure file support is always the last VCS tried
		VcsSupportFactory fileSupport = new FileVcsSupportFactory();
		vcss.put(fileSupport.getType(), fileSupport);
	}

	public Vcs getVcs(final String type, Configuration configuration) {
		VcsSupportFactory vcsFactory = vcss.get(type);
		if (vcsFactory == null) {
			throw new PrideException("No support for VCS type \"" + type + "\"");
		}

		return new Vcs(type, vcsFactory.createVcsSupport(configuration));
	}

	public Vcs findSupportingVcs(File directory, Configuration configuration) {
		for (VcsSupportFactory factory : vcss.values()) {
			if (factory.canSupport(directory)) {
				return new Vcs(factory.getType(), factory.createVcsSupport(configuration));
			}
		}
		throw new PrideException("No VCS support found for local repository in directory \"" + directory + "\"");
	}

	public Set<String> getSupportedTypes() {
		return Collections.unmodifiableSet(vcss.keySet());
	}
}
