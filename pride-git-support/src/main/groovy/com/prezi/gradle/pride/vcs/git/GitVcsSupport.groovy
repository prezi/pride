package com.prezi.gradle.pride.vcs.git

import com.prezi.gradle.pride.ProcessUtils
import com.prezi.gradle.pride.vcs.VcsSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 24/04/14.
 */
class GitVcsSupport implements VcsSupport {
	private static final Logger log = LoggerFactory.getLogger(GitVcsSupport)

	@Override
	void checkout(String repositoryUrl, File targetDirectory, boolean mirrored) {
		targetDirectory.parentFile.mkdirs()
		// Make sure we delete symlinks and directories alike
		targetDirectory.delete() || targetDirectory.deleteDir()

		log.debug "Cloning ${repositoryUrl} into ${targetDirectory}"
		def commandLine = ["git", "clone", repositoryUrl, targetDirectory]
		if (mirrored) {
			commandLine.add "--mirror"
		}
		ProcessUtils.executeIn(null, commandLine)
	}

	@Override
	void update(String repositoryUrl, File targetDirectory, boolean mirrored) {
		ProcessUtils.executeIn(targetDirectory, ["git", "fetch", "--all"])
	}

	@Override
	void activate(String repositoryUrl, File targetDirectory) {
		ProcessUtils.executeIn(targetDirectory, ["git", "remote", "set-url", "origin", repositoryUrl])
	}

	@Override
	boolean isMirroringSupported() {
		return true
	}

	@Override
	String normalizeRepositoryUrl(String repositoryUrl) {
		return repositoryUrl.replaceAll(/(\.git)|\/$/, "")
	}
}
