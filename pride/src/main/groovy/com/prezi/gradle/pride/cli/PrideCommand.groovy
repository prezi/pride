package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
abstract class PrideCommand implements Runnable {
	protected final Configuration configuration = new Configuration()

	@Option(name = ["-p", "--pride-directory"], title = "directory",
			description = "Initializes the pride in the given directory instead of the current directory")
	private File explicitPrideDirectory

	protected File getPrideDirectory() {
		explicitPrideDirectory ?: new File(System.getProperty("user.dir"))
	}

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
