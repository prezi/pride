package com.prezi.gradle.pride.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.cli.commands.AbstractCommand;
import com.prezi.gradle.pride.cli.commands.AddCommand;
import com.prezi.gradle.pride.cli.commands.ConfigCommand;
import com.prezi.gradle.pride.cli.commands.DoCommand;
import com.prezi.gradle.pride.cli.commands.InitCommand;
import com.prezi.gradle.pride.cli.commands.ListCommand;
import com.prezi.gradle.pride.cli.commands.RemoveCommand;
import com.prezi.gradle.pride.cli.commands.RmCommandAlias;
import com.prezi.gradle.pride.cli.commands.UpdateCommand;
import com.prezi.gradle.pride.cli.commands.VersionCommand;
import io.airlift.command.Cli;
import io.airlift.command.Help;
import io.airlift.command.ParseException;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class PrideCli {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PrideCli.class);

	@SuppressWarnings("unchecked")
	public static void main(String... args) {
		Cli.CliBuilder<Callable<?>> builder = Cli.builder("pride");
		builder
				.withDescription("manages a pride of modules")
				.withDefaultCommand(Help.class)
				.withCommands(
						AddCommand.class,
						ConfigCommand.class,
						DoCommand.class,
						InitCommand.class,
						ListCommand.class,
						RemoveCommand.class,
						RmCommandAlias.class,
						UpdateCommand.class,
						VersionCommand.class,
						Help.class
				);

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
				System.exit(-1);
			}

			boolean verbose = false;
			try {
				if (callable instanceof AbstractCommand) {
					AbstractCommand command = (AbstractCommand) callable;
					Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
					if (command.verbose) {
						rootLogger.setLevel(Level.DEBUG);
						verbose = true;
					} else if (command.quiet) {
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
		System.exit(exitValue);
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
