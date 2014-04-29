package com.prezi.gradle.pride.vcs.git

import com.prezi.gradle.pride.vcs.VcsSupport
import com.prezi.gradle.pride.vcs.VcsSupportFactory
import org.apache.commons.configuration.Configuration

/**
 * Created by lptr on 24/04/14.
 */
class GitVcsSupportFactory implements VcsSupportFactory {

	@Override
	String getType() {
		return "git"
	}

	@Override
	VcsSupport createVcsSupport(Configuration configuration) {
		return new GitVcsSupport(configuration)
	}
}
