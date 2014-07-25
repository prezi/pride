package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.util.List;

@Command(name = "config", description = "Set configuration parameters")
public class ConfigCommand extends AbstractCommand {

	@Option(name = "--default",
			description = "Only set the option if it is not set already")
	private boolean explicitDefault;

	@Option(name = "--global",
			description = "Get or set global configuration")
	private boolean explicitGlobal;

	@Option(name = "--local",
			description = "Get or set local configuration")
	private boolean explicitLocal;

	@Option(name = "--unset",
			description = "Unset the specified property")
	private boolean explicitUnset;

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	@Arguments(required = true,
			title = "key [<value>]",
			description = "Configuration name to read, name and value to set")
	private List<String> args;

	@Override
	public Integer call() throws Exception {
		if (args.size() < 1 || args.size() > 2) {
			throw new PrideException("Invalid number of arguments: either specify a configuration property name to read the value of the property, or a name and a value to set it.");
		}
		if (explicitUnset && args.size() != 1) {
			throw new PrideException("Invalid number of arguments: one argument is required when unsetting a configuration property.");
		}

		boolean readConfig = args.size() == 1 && !explicitUnset;
		boolean useGlobal = explicitGlobal || (!explicitLocal && !Pride.containsPride(getPrideDirectory()));

		PropertiesConfiguration globalConfiguration = loadGlobalConfiguration();
		FileConfiguration fileConfiguration;
		if (useGlobal) {
			fileConfiguration = globalConfiguration;
		} else {
			Pride pride = Pride.getPride(getPrideDirectory(), globalConfiguration, getVcsManager());
			fileConfiguration = pride.getLocalConfiguration();
		}

		String property = args.get(0);
		int result;
		if (readConfig) {
			String value = fileConfiguration.getString(property, null);
			if (value == null && !useGlobal && !explicitLocal) {
				value = globalConfiguration.getString(property, null);
			}
			if (value != null) {
				logger.info(value);
				result = 0;
			} else {
				result = 1;
			}
		} else {
			boolean changed = false;
			if (explicitUnset) {
				if (fileConfiguration.containsKey(property)) {
					fileConfiguration.clearProperty(property);
					changed = true;
				}
			} else {
				if (!explicitDefault || !fileConfiguration.containsKey(property)) {
					String value = args.get(1);
					fileConfiguration.setProperty(property, value);
					changed = true;
				}
			}
			if (changed) {
				try {
					fileConfiguration.save();
				} catch (ConfigurationException e) {
					throw new PrideException("Could not save configuration: " + e.getMessage(), e);
				}
			}
			result = changed ? 0 : 1;
		}
		return result;
	}
}
