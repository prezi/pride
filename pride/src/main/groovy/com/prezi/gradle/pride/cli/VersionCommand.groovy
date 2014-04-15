package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideVersion
import io.airlift.command.Command

/**
 * Created by lptr on 15/04/14.
 */
@Command(name = "version", description = "Display program version")
class VersionCommand implements Runnable {

	@Override
	void run() {
		System.out.println("Pride version ${PrideVersion.VERSION}")
	}
}
