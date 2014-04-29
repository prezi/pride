package com.prezi.gradle.pride.vcs

import org.apache.commons.configuration.Configuration

/**
 * Created by lptr on 24/04/14.
 */
interface VcsSupportFactory {
	/**
	 * Returns the type of the supported VCS.
	 */
	String getType()

	/**
	 * Creates the {@link VcsSupport}.
	 */
	VcsSupport createVcsSupport(Configuration configuration)
}
