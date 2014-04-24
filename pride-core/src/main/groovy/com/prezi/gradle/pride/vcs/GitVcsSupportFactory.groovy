package com.prezi.gradle.pride.vcs

/**
 * Created by lptr on 24/04/14.
 */
class GitVcsSupportFactory implements VcsSupportFactory {

	@Override
	String getType() {
		return "git"
	}

	@Override
	VcsSupport createVcsSupport() {
		return new GitVcsSupport()
	}
}
