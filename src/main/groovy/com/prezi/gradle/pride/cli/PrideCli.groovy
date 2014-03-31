package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import io.airlift.command.Cli
import io.airlift.command.Command
import io.airlift.command.Help
import io.airlift.command.ParseException

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
				.withCommands(AddToSession, InitSession, ListSession, Help)

		Cli<Runnable> parser = builder.build();
		try {
			def command = parser.parse(args)
			command.run();
		} catch (ParseException e) {
			showError(e)
		} catch (PrideException e) {
			showError(e)
		} catch (Exception e) {
			e.printStackTrace()
			System.exit(-1)
		}
	}

	private static void showError(Exception e) {
		System.err.println("ERROR: ${e.message}")
		System.exit(-1)
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
