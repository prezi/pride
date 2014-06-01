package com.prezi.gradle.pride.vcs

/**
 * Created by lptr on 24/04/14.
 */
interface VcsSupport {
	void checkout(String repositoryUrl, File targetDirectory, boolean mirrored)

	/**
	 * Updates a local clone of a repository. If there are local changes, they should be
	 * reserved. If the {@code mirrored} property is set, this is a cached mirror clone,
	 * and local changes should not be expected.
	 *
	 * @param targetDirectory
	 * 		The directory where the local clone resides.
	 * @param mirrored
	 * 		Whether to update a real clone or a cached repository.
	 */
	void update(File targetDirectory, boolean mirrored)

    /**
     * Returns true if the local clone has uncommitted/pushed changes.
     * @param targetDirectory
     * @return
     */
    boolean hasChanges(File targetDirectory)

	/**
	 * Activates a clone just cloned form a cache to work as if it was cloned from
	 * the original URL. In Git this would do {@code git remote set-url origin <repositoryUrl>}.
	 * @param repositoryUrl The URL of the remote repository
	 * @param targetDirectory The directory where the local clone resides.
	 */
	void activate(String repositoryUrl, File targetDirectory)

	/**
	 * Returns {@code true} if the VCS supports local clones of repositories that
	 * can be cloned further. If the VCS supports mirroring, mirrored clones of
	 * repositories will be used as local caches to speed up pride creation.
	 */
	boolean isMirroringSupported()

	/**
	 * Normalizes the repository URL, e.g. removes trailing slashes etc.
	 */
	String normalizeRepositoryUrl(String repositoryUrl)

	/**
	 * Resolves the repository name from a repository URL if possible.
	 * @return
	 * 		The module name or {@code null} if the repository name is not resolvable.
	 */
	String resolveRepositoryName(String repositoryUrl)
}
