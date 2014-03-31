package com.prezi.gradle.pride.cli

import io.airlift.command.Cli
import io.airlift.command.Command
import io.airlift.command.Help
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
public class PrideCli {
	public static void main(String... args) {
		Cli.CliBuilder<Runnable> builder = Cli.<Runnable> builder("pride")
				.withDescription("manages a pride of modules")
				.withDefaultCommand(Help.class)
				.withCommands(Help.class, Init.class);
		Cli<Runnable> gitParser = builder.build();
		gitParser.parse(args).run();
	}
}

abstract class SessionCommand implements Runnable {
	@Option(name = "-s", description = "session directory")
	private File explicitSessionDirectory

	protected File getSessionDirectory() {
		explicitSessionDirectory ?: new File(System.getProperty("user.dir"))
	}
}

@Command(name = "init", description = "Initialize session")
class Init extends SessionCommand {

	@Override
	public void run() {
		System.out.println("Initializing ${sessionDirectory}")
	}
}
