package com.prezi.gradle.pride.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.commands.Group;
import com.prezi.gradle.pride.cli.commands.PrideCommand;
import io.airlift.command.Cli;
import io.airlift.command.Help;
import io.airlift.command.ParseException;
import io.airlift.command.SuggestCommand;
import io.airlift.command.model.CommandGroupMetadata;
import io.airlift.command.model.CommandMetadata;
import io.airlift.command.model.GlobalMetadata;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

public class PrideCli {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PrideCli.class);

	public static void main(String... args) {
		System.exit(execute(args));
	}

	@SuppressWarnings("unchecked")
	public static int execute(String... args) {
		Cli.CliBuilder<Callable<?>> builder = Cli.builder("pride");
		builder
				.withDescription("manages a pride of modules")
				.withDefaultCommand(Help.class)
				.withCommands(
						Help.class,
						SuggestCommand.class
				);

		for (PridePlugin plugin : ServiceLoader.load(PridePlugin.class)) {
			for (Class<? extends PrideCommand> command : plugin.getCommands()) {
				Group group = command.getAnnotation(Group.class);
				if (group != null) {
					builder.withGroup(group.value()).withCommand(command);
				} else {
					builder.withCommand(command);
				}
			}
		}

		Cli<Callable<?>> parser = builder.build();
		int exitValue;
		try {
			Callable<?> callable = null;
			try {
				callable = parser.parse(args);
			} catch (ParseException e) {
				if (ArrayUtils.contains(args, "-v") || ArrayUtils.contains(args, "--verbose")) {
					throw e;
				}

				logger.error("{}", e.getMessage());
				return -1;
			}

			boolean verbose = false;
			try {
				if (callable instanceof PrideCommand) {
					PrideCommand command = (PrideCommand) callable;
					if (command.isHelp()) {
						CommandMetadata commandMetadata = findCommandMetadata(parser.getMetadata(), command.getClass());
						Help.help(commandMetadata);
						return 0;
					}
					Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
					if (command.isVerbose()) {
						rootLogger.setLevel(Level.DEBUG);
						verbose = true;
					} else if (command.isQuiet()) {
						rootLogger.setLevel(Level.WARN);
					}
				}
				Object result = callable.call();
				if (result instanceof Integer) {
					exitValue = (Integer) result;
				} else {
					exitValue = 0;
				}
			} catch (PrideException e) {
				if (verbose) {
					throw e;
				}

				logPrideExceptions(e);
				exitValue = -1;
			}
		} catch (Exception e) {
			logger.error("Exception:", e);
			exitValue = -1;
		}

		return exitValue;
	}

	private static CommandMetadata findCommandMetadata(GlobalMetadata global, Class<? extends PrideCommand> type) {
		CommandMetadata result = findCommandMetadata(null, global.getDefaultGroupCommands(), type);
		result = findCommandMetadata(result, global.getDefaultCommand(), type);
		for (CommandGroupMetadata group : global.getCommandGroups()) {
			result = findCommandMetadata(result, group.getCommands(), type);
			result = findCommandMetadata(result, group.getDefaultCommand(), type);
		}
		return result;
	}

	private static CommandMetadata findCommandMetadata(CommandMetadata result, List<CommandMetadata> commands, Class<? extends PrideCommand> type) {
		for (CommandMetadata command : commands) {
			result = findCommandMetadata(result, command, type);
		}
		return result;
	}

	private static CommandMetadata findCommandMetadata(CommandMetadata result, CommandMetadata command, Class<? extends PrideCommand> type) {
		if (result != null) {
			return result;
		}
		if (command.getType().equals(type)) {
			return command;
		}
		return null;
	}

	private static void logPrideExceptions(Throwable t) {
		if (t != null) {
			logPrideExceptions(t.getCause());
			if (t instanceof PrideException) {
				logger.error("{}", t.getMessage());
			}
		}
	}
}
