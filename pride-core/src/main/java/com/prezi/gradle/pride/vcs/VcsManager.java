package com.prezi.gradle.pride.vcs;

import com.prezi.gradle.pride.PrideException;
import org.apache.commons.configuration.Configuration;

import java.util.*;

public final class VcsManager {

	private final Map<String, VcsSupportFactory> vcss = new LinkedHashMap<String, VcsSupportFactory>();

	public VcsManager() {
		for (VcsSupportFactory factory : ServiceLoader.load(VcsSupportFactory.class)) {
			vcss.put(factory.getType(), factory);
		}
	}

	public Vcs getVcs(final String type, Configuration configuration) {
		VcsSupportFactory vcsFactory = vcss.get(type);
		if (vcsFactory == null) {
			throw new PrideException("No support for VCS type \"" + type + "\"");
		}

		return new Vcs(type, vcsFactory.createVcsSupport(configuration));
	}

	public Set<String> getSupportedTypes() {
		return Collections.unmodifiableSet(vcss.keySet());
	}
}
