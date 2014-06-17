package com.prezi.gradle.pride.vcs;

import java.io.File;
import java.io.IOException;

public interface VcsSupport {
	void checkout(String repositoryUrl, File targetDirectory, boolean mirrored) throws IOException;

	/**
	 * Updates a local clone of a repository. If there are local changes, they should be
	 * reserved. If the {@code mirrored} property is set, this is a cached mirror clone,
	 * and local changes should not be expected.
	 *
	 * @param targetDirectory The directory where the local clone resides.
	 * @param mirrored        Whether to update a real clone or a cached repository.
	 * @throws java.io.IOException If an I/O error occurs.
	 */
	void update(File targetDirectory, boolean mirrored) throws IOException;

	/**
	 * Activates a clone just cloned form a cache to work as if it was cloned from
	 * the original URL. In Git this would do {@code git remote set-url origin <repositoryUrl>}.
	 *
	 * @param repositoryUrl   The URL of the remote repository
	 * @param targetDirectory The directory where the local clone resides.
	 * @throws java.io.IOException If an I/O error occurs.
	 */
	void activate(String repositoryUrl, File targetDirectory) throws IOException;

	/**
	 * Returns {@code true} if the VCS supports local clones of repositories that
	 * can be cloned further. If the VCS supports mirroring, mirrored clones of
	 * repositories will be used as local caches to speed up pride creation.
	 *
	 * @return true if mirroring is supported, false otherwise.
	 */
	boolean isMirroringSupported();

	/**
	 * Normalizes the repository URL, e.g. removes trailing slashes etc.
	 *
	 * @param repositoryUrl the URL to normalize.
	 * @return the normalized URL.
	 */
	String normalizeRepositoryUrl(String repositoryUrl);

	/**
	 * Resolves the repository name from a the user input if it contains a valid repository URL.
	 *
	 * @param repository the repository string supplied by the user.
	 * @return The module name or {@code null} if the repository name is not resolvable.
	 */
	String resolveRepositoryName(String repository);
}
