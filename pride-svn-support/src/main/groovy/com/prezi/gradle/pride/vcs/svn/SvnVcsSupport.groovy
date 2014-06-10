package com.prezi.gradle.pride.vcs.svn

import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.ProcessUtils
import com.prezi.gradle.pride.vcs.VcsSupport
import org.apache.commons.configuration.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by jzwolak on 01/06/14.
 */
class SvnVcsSupport implements VcsSupport {

	private static final Logger log = LoggerFactory.getLogger(SvnVcsSupport)
	private final Configuration configuration

	SvnVcsSupport(Configuration configuration) {
		this.configuration = configuration
	}

	@Override
	void checkout(String repositoryUrl, File targetDirectory, boolean mirrored) {
		targetDirectory.parentFile.mkdirs()
		// Make sure we delete symlinks and directories alike
		targetDirectory.delete() || targetDirectory.deleteDir()

		log.debug "Checking out ${repositoryUrl} into ${targetDirectory}"
		def commandLine = ["svn", "checkout", repositoryUrl, targetDirectory]
		ProcessUtils.executeIn(null, commandLine)
	}

	@Override
	void update(File targetDirectory, boolean mirrored) {
		def updateCommand = ["svn", "update"]
		ProcessUtils.executeIn(targetDirectory, updateCommand)
	}

	@Override
	boolean hasChanges(File targetDirectory) {
		def statusCommand = ["svn", "status"]
		def process = ProcessUtils.executeIn(targetDirectory, statusCommand, false)
		return !process.text.trim().empty
	}

	@Override
	void activate(String repositoryUrl, File targetDirectory) {
		thrown new PrideException("svn doesn't support activate")
	}

	@Override
	boolean isMirroringSupported() {
		return false
	}

	@Override
	String normalizeRepositoryUrl(String repositoryUrl) {
		return repositoryUrl
	}

	@Override
	String resolveRepositoryName(String repositoryUrl) {
		try {
			// check if the user supplied argument is a repository
			// an exception is thrown if it is not
			def commandLine = ["svn", "ls", repositoryUrl]
			def process = ProcessUtils.executeIn(null, commandLine, false)
		} catch ( PrideException ex ) {
			// user supplied argument isn't a repo. return null so it will be tried as a module name
			return null
		}
		// user supplied argument is a repo
		// try to extract the module name and return it if successful
		def m = repositoryUrl =~ /^.*?([-\._\w]+?)\\/?$/
		if (m) {
			return m[0][1]
		} else {
			return null
		}
	}
}
