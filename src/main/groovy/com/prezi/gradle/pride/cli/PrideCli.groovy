package com.prezi.gradle.pride.cli

import io.airlift.command.Cli
import io.airlift.command.Command
import io.airlift.command.Help

/**
 * Created by lptr on 31/03/14.
 */
public class PrideCli {
	public static void main(String... args) {
		Cli.CliBuilder<Runnable> builder = Cli.<Runnable> builder("pride")
				.withDescription("manages a pride of modules")
				.withDefaultCommand(Help)
				.withCommands(Version, Help)

		builder.withGroup("session")
				.withDescription("Manage sessions")
				.withDefaultCommand(ListSession)
				.withCommands(InitSession, ListSession, Help)

		Cli<Runnable> parser = builder.build();
		parser.parse(args).run();
	}
}

@Command(name = "version", description = "Display program version")
class Version implements Runnable {

	@Override
	void run() {
		def props = new Properties()
		props.load(getClass().getResourceAsStream("/version.properties"))
		System.out.println("Version ${props["application.version"]}")
	}
}
