package com.prezi.gradle.pride.vcs.git

import com.prezi.gradle.pride.ProcessUtils
import com.prezi.gradle.pride.vcs.VcsSupport
import org.apache.commons.configuration.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 24/04/14.
 */
class GitVcsSupport implements VcsSupport {
	public static final GIT_UPDATE = "git.update"

	private static final Logger log = LoggerFactory.getLogger(GitVcsSupport)
	private final Configuration configuration

	GitVcsSupport(Configuration configuration) {
		this.configuration = configuration
	}

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
	void update(File targetDirectory, boolean mirrored) {
		def fetchCommand = ["git", "fetch"]

		// Cached repositories need to update all branches
		if (mirrored) {
			fetchCommand.add "--all"
		}
		ProcessUtils.executeIn(targetDirectory, fetchCommand)

		// Update working copy unless this is a cached clone
		if (!mirrored) {
			def updateCommand = configuration.getString(GIT_UPDATE, "git rebase --autostash")
			ProcessUtils.executeIn(targetDirectory, updateCommand.tokenize(" "))
		}
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

	@Override
	String resolveRepositoryName(String repositoryUrl) {
		def m = repositoryUrl =~ /^(?:git@|(?:https?):\\/+).*[:\\/]([-\._\w]+?)(?:\.git)?\\/?$/
		if (m) {
			return m[0][1]
		} else {
			return null
		}
	}
}
