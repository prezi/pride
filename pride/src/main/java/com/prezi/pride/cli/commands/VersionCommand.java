package com.prezi.pride.cli.commands;

import com.prezi.pride.PrideVersion;
import io.airlift.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "version", description = "Display program version")
public class VersionCommand implements PrideCommand {
	private static final Logger log = LoggerFactory.getLogger(VersionCommand.class);

	@Override
	public Integer call() throws Exception {
		log.info("Pride version " + PrideVersion.VERSION);
		return 0;
	}

	@Override
	public boolean isVerbose() {
		return false;
	}

	@Override
	public boolean isQuiet() {
		return false;
	}

	@Override
	public boolean isHelp() {
		return false;
	}
}
