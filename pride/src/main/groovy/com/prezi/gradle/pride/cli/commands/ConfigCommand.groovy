package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.PrideException
import io.airlift.command.Arguments
import io.airlift.command.Command

/**
 * Created by lptr on 15/04/14.
 */
@Command(name = "config", description = "Set configuration parameters")
class ConfigCommand extends AbstractCommand {
	@Arguments(required = true, description = "Configuration name to read, name and value to set")
	private List<String> args

	@Override
	void run() {
		switch (args.size()) {
			case 1:
				log.info configuration.getParameter(args[0])
				break
			case 2:
				configuration.setParameter(args[0], args[1])
				configuration.save()
				break
			default:
				throw new PrideException("Invalid number of arguments: either specify a configuration property name to read the value of the property, or a name and a value to set it.")
		}
	}
}
