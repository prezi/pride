package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.PrideException;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.configuration.ConfigurationException;

import java.util.List;

@Command(name = "config", description = "Set configuration parameters")
public class ConfigCommand extends AbstractCommand {

	@Option(name = "--default",
			description = "Only set the option if it is not set already")
	private boolean explicitDefault;

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	@Arguments(required = true,
			title = "key [<value>]",
			description = "Configuration name to read, name and value to set")
	private List<String> args;

	@Override
	protected void runInternal() {
		switch (args.size()) {
			case 1:
				String value = globalConfiguration.getString(args.get(0), null);
				logger.info(value);
				if (value == null) {
					System.exit(1);
				}

				break;
			case 2:
				if (!explicitDefault || !globalConfiguration.containsKey(args.get(0))) {
					globalConfiguration.setProperty(args.get(0), args.get(1));
					try {
						globalConfiguration.save();
					} catch (ConfigurationException e) {
						throw new PrideException("Could not save configuration: " + e.getMessage(), e);
					}
				}

				break;
			default:
				throw new PrideException("Invalid number of arguments: either specify a configuration property name to read the value of the property, or a name and a value to set it.");
		}
	}
}
