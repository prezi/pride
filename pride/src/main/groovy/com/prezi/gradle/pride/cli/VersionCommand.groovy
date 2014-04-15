package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideVersion
import io.airlift.command.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 15/04/14.
 */
@Command(name = "version", description = "Display program version")
class VersionCommand implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(VersionCommand)

	@Override
	void run() {
		log.info "Pride version ${PrideVersion.VERSION}"
	}
}
