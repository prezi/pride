package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.PrideException
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 15/04/14.
 */
@Command(name = "config", description = "Set configuration parameters")
class ConfigCommand extends AbstractCommand {
	@Option(name = "--default",
			description = "Only set the option if it is not set already")
	private boolean explicitDefault

	@Arguments(required = true,
			title = "key [<value>]",
			description = "Configuration name to read, name and value to set")
	private List<String> args

	@Override
	void run() {
		switch (args.size()) {
			case 1:
				def value = fileConfiguration.getString(args[0], null)
				log.info value
				if (value == null) {
					System.exit(1)
				}
				break
			case 2:
				if (!explicitDefault || !fileConfiguration.containsKey(args[0])) {
					fileConfiguration.setProperty(args[0], args[1])
					fileConfiguration.save()
				}
				break
			default:
				throw new PrideException("Invalid number of arguments: either specify a configuration property name to read the value of the property, or a name and a value to set it.")
		}
	}
}
