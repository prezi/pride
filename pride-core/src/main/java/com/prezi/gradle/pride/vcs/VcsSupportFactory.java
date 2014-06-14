package com.prezi.gradle.pride.vcs;

import org.apache.commons.configuration.Configuration;

public interface VcsSupportFactory {
	/**
	 * Returns the type of the supported VCS.
	 */
	String getType();

	/**
	 * Creates the {@link com.prezi.gradle.pride.vcs.VcsSupport}.
	 */
	VcsSupport createVcsSupport(Configuration configuration);
}
