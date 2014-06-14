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

public class PrideCli {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PrideCli.class);

	public static void main(String... args) {
		Cli.CliBuilder<Runnable> builder = Cli.builder("pride");
		//noinspection unchecked
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

		Cli<Runnable> parser = builder.build();
		try {
			Runnable runnable = null;
			try {
				runnable = parser.parse(args);
			} catch (ParseException e) {
				if (ArrayUtils.contains(args, "-v") || ArrayUtils.contains(args, "--verbose")) {
					throw e;
				}

				logger.error("{}", e.getMessage());
				System.exit(-1);
			}

			boolean verbose = false;
			try {
				if (runnable instanceof AbstractCommand) {
					AbstractCommand command = (AbstractCommand) runnable;
					Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
					if (command.verbose) {
						rootLogger.setLevel(Level.DEBUG);
						verbose = true;
					} else if (command.quiet) {
						rootLogger.setLevel(Level.WARN);
					}
				}
				runnable.run();
			} catch (PrideException e) {
				if (verbose) {
					throw e;
				}

				logPrideExceptions(e);
				System.exit(-1);
			}
		} catch (Exception e) {
			logger.error("Exception:", e);
			System.exit(-1);
		}
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
