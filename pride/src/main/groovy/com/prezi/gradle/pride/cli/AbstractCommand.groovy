package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import io.airlift.command.Option
import io.airlift.command.OptionType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 15/04/14.
 */
abstract class AbstractCommand implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(AbstractCommand)

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

	protected static Process executeIn(File directory, List<?> commandLine, boolean processOutput = true, boolean redirectErrorStream = true) {
		def stringCommandLine = commandLine.collect { String.valueOf(it) }
		log.debug "Executing ${stringCommandLine.join(" ")} in ${directory ?: System.getProperty("user.dir")}"
		def builder = new ProcessBuilder(stringCommandLine)
		builder.directory(directory)
		if (redirectErrorStream) {
			builder.redirectErrorStream(true)
		}
		def process = builder.start()

		if (processOutput) {
			process.errorStream.eachLine {
				log.warn "{}", it
			}
			process.inputStream.eachLine {
				log.info "{}", it
			}
		}
		process.waitFor()
		def result = process.exitValue()
		if (result) {
			throw new PrideException("Failed to execute \"${commandLine.join(" ")}\" in \"${directory}\", exit code: ${result}" +
					(processOutput ? "" : "\n${process.text}"))
		}
		return process
	}
}
