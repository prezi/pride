package com.prezi.pride;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessUtils {

	private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

	public static Process executeIn(File directory, List<?> commandLine, boolean processOutput, boolean redirectErrorStream, List<Integer> acceptableExitCodes) throws IOException {
		List<String> stringCommandLine = new ArrayList<String>();
		for (Object item : commandLine) {
			stringCommandLine.add(String.valueOf(item));
		}
		logger.debug("Executing in {}: {}", directory != null ? directory : System.getProperty("user.dir"), StringUtils.join(stringCommandLine, " "));
		ProcessBuilder builder = new ProcessBuilder(stringCommandLine);
		builder.directory(directory);
		if (redirectErrorStream) {
			builder.redirectErrorStream(true);
		}

		Process process = builder.start();

		if (processOutput) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			try {
				String line;
				while ((line = errorReader.readLine()) != null) {
					logger.warn("{}", line);
				}
			} finally {
				errorReader.close();
			}
			BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			try {
				String line;
				while ((line = outputReader.readLine()) != null) {
					logger.info("{}", line);
				}
			} finally {
				outputReader.close();
			}
		}

		try {
			process.waitFor();
		} catch (InterruptedException e) {
			throw new IOException("Interrupted", e);
		}
		int result = process.exitValue();
		if (!acceptableExitCodes.contains(result)) {
			String output;
			if (!processOutput) {
				List<String> outputLines = new ArrayList<String>();
				outputLines.addAll(IOUtils.readLines(process.getErrorStream()));
				outputLines.addAll(IOUtils.readLines(process.getInputStream()));
				output = ", output:\n" + StringUtils.join(outputLines, '\n');
			} else {
				output = "";
			}
			throw new PrideException("Failed to execute \"" + StringUtils.join(commandLine, " ") + "\" in \"" + directory + "\", exit code: " + result + output);
		}

		return process;
	}

	public static Process executeIn(File directory, List<?> commandLine, boolean processOutput, boolean redirectErrorStream) throws IOException {
		List<Integer> defaultAcceptableExitCodes = Arrays.asList(0);
		return ProcessUtils.executeIn(directory, commandLine, processOutput, redirectErrorStream, defaultAcceptableExitCodes);
	}

	public static Process executeIn(File directory, List<?> commandLine, boolean processOutput) throws IOException {
		return ProcessUtils.executeIn(directory, commandLine, processOutput, true);
	}

	public static Process executeIn(File directory, List<?> commandLine) throws IOException {
		return ProcessUtils.executeIn(directory, commandLine, true, true);
	}
}
