package com.prezi.pride.vcs;

import org.apache.commons.configuration.Configuration;

import java.io.File;

public interface VcsSupportFactory {
	/**
	 * Returns the type of the supported VCS.
	 *
	 * @return the type identifier of this factory.
	 */
	String getType();

	/**
	 * Creates the {@link VcsSupport}.
	 *
	 * @param configuration the configuration to use to set up the VCS support.
	 * @return the actual VCS support instance.
	 */
	VcsSupport createVcsSupport(Configuration configuration);

	/**
	 * Checks if this factory can support a local clone in the given directory.
	 *
	 * @param targetDirectory    The directory where the local clone resides.
	 * @return {@code true} when supported, {@code false} otherwise.
	 */
	boolean canSupport(File targetDirectory);
}
