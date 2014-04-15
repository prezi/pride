package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import io.airlift.command.Option
import io.airlift.command.OptionType

/**
 * Created by lptr on 15/04/14.
 */
abstract class AbstractCommand implements Runnable {
	@Option(type = OptionType.GLOBAL,
			name = ["-v", "--verbose"],
			description = "Verbose mode")
	public boolean verbose

	@Option(type = OptionType.GLOBAL,
			name = ["-q", "--quiet"],
			description = "Quite mode")
	public boolean quiet

	protected final Configuration configuration = new Configuration()

	protected static Process execute(Object... commandLine) {
		executeIn(null, commandLine.toList(), true)
	}

	protected static Process executeIn(File directory, List<?> commandLine, boolean processOutput = true) {
		def process = commandLine.collect { String.valueOf(it) }.execute((String[]) null, directory)
		if (processOutput) {
			process.waitForProcessOutput((OutputStream) System.out, System.err)
		} else {
			process.waitFor()
		}
		def result = process.exitValue()
		if (result) {
			throw new PrideException("Failed to execute \"${commandLine.join(" ")}\" in \"${directory}\", exit code: ${result}" +
					(processOutput ? "" : "\n${process.text}"))
		}
		return process
	}
}
