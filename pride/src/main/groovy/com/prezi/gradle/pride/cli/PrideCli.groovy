package com.prezi.gradle.pride.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.prezi.gradle.pride.PrideException
import io.airlift.command.Cli
import io.airlift.command.Help
import io.airlift.command.ParseException
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 31/03/14.
 */
public class PrideCli {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PrideCli)

	public static void main(String... args) {
		Cli.CliBuilder<Runnable> builder = Cli.<Runnable> builder("pride")
				.withDescription("manages a pride of modules")
				.withDefaultCommand(Help)
				.withCommands(
					AddToPrideCommand,
					ConfigCommand,
					DoInPrideCommand,
					InitPrideCommand,
					RemoveFromPrideCommand,
					RmCommandAlias,
					VersionCommand,
					Help)

		Cli<Runnable> parser = builder.build();
		try {
			Runnable command = null
			try {
				command = parser.parse(args)
			} catch (ParseException e) {
				if (args.contains("-v") || args.contains("--verbose")) {
					throw e
				}
				log.error "{}", e.message
				System.exit(-1)
			}

			def verbose = false
			try {
				if (command instanceof AbstractCommand) {
					Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
					if (command.verbose) {
						rootLogger?.setLevel(Level.DEBUG)
						verbose = true
					} else if (command.quiet) {
						rootLogger?.setLevel(Level.WARN)
					}
				}
				command.run()
			} catch (PrideException e) {
				if (verbose) {
					throw e
				}
				log.error "{}", e.message
				System.exit(-1)
			}
		} catch (Exception e) {
			log.error "Exception:", e
			System.exit(-1)
		}
	}
}
