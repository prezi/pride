package com.prezi.gradle.pride.vcs.svn

import com.prezi.gradle.pride.vcs.VcsSupport
import com.prezi.gradle.pride.vcs.VcsSupportFactory
import org.apache.commons.configuration.Configuration

/**
 * Created by jzwolak on 01/06/14.
 */
class SvnVcsSupportFactory implements VcsSupportFactory {

	@Override
	String getType() {
		return "svn"
	}

	@Override
	VcsSupport createVcsSupport(Configuration configuration) {
		return new SvnVcsSupport(configuration)
	}
}
