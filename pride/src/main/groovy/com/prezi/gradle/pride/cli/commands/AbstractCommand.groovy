package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.cli.Configuration
import io.airlift.command.Option
import io.airlift.command.OptionType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 15/04/14.
 */
abstract class AbstractCommand implements Runnable {
	protected static final Logger log = LoggerFactory.getLogger(AbstractCommand)

	@Option(type = OptionType.GLOBAL,
			name = ["-v", "--verbose"],
			description = "Verbose mode")
	public boolean verbose

	@Option(type = OptionType.GLOBAL,
			name = ["-q", "--quiet"],
			description = "Quite mode")
	public boolean quiet

	protected final Configuration configuration = new Configuration()
}
