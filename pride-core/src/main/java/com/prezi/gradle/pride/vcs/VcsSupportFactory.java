package com.prezi.gradle.pride.vcs;

import org.apache.commons.configuration.Configuration;

public interface VcsSupportFactory {
	/**
	 * Returns the type of the supported VCS.
	 *
	 * @return the type identifier of this factory.
	 */
	String getType();

	/**
	 * Creates the {@link com.prezi.gradle.pride.vcs.VcsSupport}.
	 *
	 * @param configuration the configuration to use to set up the VCS support.
	 * @return the actual VCS support instance.
	 */
	VcsSupport createVcsSupport(Configuration configuration);
}
