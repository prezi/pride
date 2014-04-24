package com.prezi.gradle.pride

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by lptr on 18/04/14.
 */
class ProcessUtils {
	private static final Logger log = LoggerFactory.getLogger(ProcessUtils)

	public static Process executeIn(File directory, List<?> commandLine, boolean processOutput = true, boolean redirectErrorStream = true) {
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
