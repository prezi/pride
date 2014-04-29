package com.prezi.gradle.pride.vcs

import com.prezi.gradle.pride.PrideException
import org.apache.commons.configuration.Configuration

/**
 * Created by lptr on 24/04/14.
 */
final class VcsManager {
	private final Map<String, VcsSupportFactory> vcss = [:]

	VcsManager() {
		ServiceLoader.load(VcsSupportFactory).each { factory ->
			vcss.put(factory.type, factory)
		}
	}

	public Vcs getVcs(String type, Configuration configuration) {
		def vcsFactory = vcss.get(type)
		if (vcsFactory == null) {
			throw new PrideException("No support for VCS type \"${type}\"")
		}
		return new Vcs(type, vcsFactory.createVcsSupport(configuration))
	}

	public Set<String> getSupportedTypes() {
		return vcss.keySet().asImmutable()
	}
}
