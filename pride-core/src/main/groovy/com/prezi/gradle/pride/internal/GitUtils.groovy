package com.prezi.gradle.pride.internal

import com.prezi.gradle.pride.ProcessUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 18/04/14.
 */
class GitUtils {
	private static final Logger log = LoggerFactory.getLogger(GitUtils)

	public static void cloneRepository(String repositoryUrl, File targetDirectory, boolean mirror = false) {
		targetDirectory.parentFile.mkdirs()
		// Make sure we delete symlinks and directories alike
		targetDirectory.delete() || targetDirectory.deleteDir()

		log.debug "Cloning ${repositoryUrl} into ${targetDirectory}"
		def commandLine = ["git", "clone", repositoryUrl, targetDirectory]
		if (mirror) {
			commandLine.add "--mirror"
		}
		ProcessUtils.executeIn(null, commandLine)
	}

	public static void setOrigin(String repositoryUrl, File targetDirectory) {
		ProcessUtils.executeIn(targetDirectory, ["git", "remote", "set-url", "origin", repositoryUrl])
	}
}
