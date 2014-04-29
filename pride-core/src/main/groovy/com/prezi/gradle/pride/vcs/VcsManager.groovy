package com.prezi.gradle.pride.vcs

import com.prezi.gradle.pride.PrideException

/**
 * Created by lptr on 24/04/14.
 */
final class VcsManager {
	private final Map<String, VcsSupport> vcss = [:]

	VcsManager() {
		ServiceLoader.load(VcsSupportFactory).each { factory ->
			vcss.put(factory.type, factory.createVcsSupport())
		}
	}

	public Vcs getVcs(String type) {
		def vcsSupport = vcss.get(type)
		if (vcsSupport == null) {
			throw new PrideException("No support for VCS type \"${type}\"")
		}
		return new Vcs(type, vcsSupport)
	}

	public Set<String> getSupportedTypes() {
		return vcss.keySet().asImmutable()
	}
}
