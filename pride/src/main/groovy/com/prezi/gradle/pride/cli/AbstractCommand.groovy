package com.prezi.gradle.pride.cli

import io.airlift.command.Option
import io.airlift.command.OptionType

/**
 * Created by lptr on 15/04/14.
 */
abstract class AbstractCommand implements Runnable {
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
