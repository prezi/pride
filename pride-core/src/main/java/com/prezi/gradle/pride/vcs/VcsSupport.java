package com.prezi.gradle.pride.vcs;

import java.io.File;
import java.io.IOException;

public interface VcsSupport {
	/**
	 * Clone a repository into the pride.
	 *
	 * @param repositoryUrl   The URL of the remote repository.
	 * @param targetDirectory The directory where the local clone will reside.
	 * @param branch          The branch to check out.
	 * @param recursive       Clone sub-repositories as well.
	 * @param mirrored        Whether to create a mirror (to be used as a cache).
	 * @throws java.io.IOException If an I/O error occurs.
	 */
	void checkout(String repositoryUrl, File targetDirectory, String branch, boolean recursive, boolean mirrored) throws IOException;

	/**
	 * Updates a local clone of a repository. If there are local changes, they should be
	 * reserved. If the {@code mirrored} property is set, this is a cached mirror clone,
	 * and local changes should not be expected.
	 *
	 * @param targetDirectory The directory where the local clone resides.
	 * @param recursive       Update sub-repositories as well.
	 * @param mirrored        Whether to update a real clone or a cached repository.
	 * @throws java.io.IOException If an I/O error occurs.
	 */
	void update(File targetDirectory, boolean recursive, boolean mirrored) throws IOException;

	/**
	 * Returns true if the working copy / local repository has uncommitted or unpublished changes.
	 *
	 * @param targetDirectory The directory where the local clone resides.
	 * @return {@code true} if there are changes in the local clone.
	 * @throws java.io.IOException If an I/O error occurs.
	 */
	boolean hasChanges(File targetDirectory) throws IOException;

	/**
	 * Returns the status of the given directory.
	 *
	 * @param targetDirectory The directory where the local clone resides.
	 * @return the status of <code>targetDirectory</code>.
	 * @throws java.io.IOException If an I/O error occurs.
	 */
	VcsStatus getStatus(File targetDirectory) throws IOException;

	/**
	 * Activates a clone just cloned form a cache to work as if it was cloned from
	 * the original URL. In Git this would do {@code git remote set-url origin <repositoryUrl>}.
	 *
	 * @param repositoryUrl   The URL of the remote repository.
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
	 * Returns the remote URL of the local clone.
	 *
	 * @param targetDirectory The directory where the local clone resides.
	 * @return the URL of the repository, or {@code null} if one cannot be inferred.
	 * @throws java.io.IOException If an I/O error occurs.
	 */
	String getRepositoryUrl(File targetDirectory) throws IOException;

	/**
	 * Returns the branch name of the current working copy.
	 * @return the branch name of {@code null} if not on a branch.
	 * @throws java.io.IOException If an I/O error occurs.
	 */
	String getBranch(File targetDirectory) throws IOException;

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
