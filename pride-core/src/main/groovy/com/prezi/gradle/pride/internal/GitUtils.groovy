package com.prezi.gradle.pride.internal

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 18/04/14.
 */
class GitUtils {
	private static final Logger log = LoggerFactory.getLogger(GitUtils)

	public static File cloneRepository(String repository, File targetDirectory, boolean mirror) {
		targetDirectory.parentFile.mkdirs()
		// Make sure we delete symlinks and directories alike
		targetDirectory.delete() || targetDirectory.deleteDir()

		log.info "Cloning ${repository} into ${targetDirectory}"
		def commandLine = ["git", "clone", repository, targetDirectory]
		if (mirror) {
			commandLine.add "--mirror"
		}
		ProcessUtils.executeIn(null, commandLine)
		return targetDirectory
	}
}
