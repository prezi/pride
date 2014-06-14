package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.PrideVersion;
import io.airlift.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "version", description = "Display program version")
public class VersionCommand implements Runnable {
	@Override
	public void run() {
		log.info("Pride version " + PrideVersion.VERSION);
	}

	private static final Logger log = LoggerFactory.getLogger(VersionCommand.class);
}
