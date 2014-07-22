package com.prezi.gradle.pride.vcs.git;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import com.prezi.gradle.pride.ProcessUtils;
import com.prezi.gradle.pride.vcs.VcsSupport;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitVcsSupport implements VcsSupport {
	public static final String GIT_UPDATE = "git.update";

	private static final Logger log = LoggerFactory.getLogger(GitVcsSupport.class);
	private static final Pattern REPOSITORY_URL_PATTERN = Pattern.compile("^"
				+ "(?:"
					+ "\\w+@.+:(?:.+/)?"					//	SCP-like pattern prefix
				+ "|"
					+ "(?:git|https?|ftps?|rsync|ssh)://"	// Protocol prefix
					+ ".+/"									// path to repo
				+ ")"
				+ "(.+?)" 									// repo name
				+ "(?:\\.git)?"								// optional .git suffix
				+ "/?"										// optional trailing slash
				+ "$", Pattern.COMMENTS);
	private static final Pattern STATUS_AHEAD = Pattern.compile(".*\\[ahead \\d+\\]");

	private final Configuration configuration;

	GitVcsSupport(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void checkout(String repositoryUrl, File targetDirectory, boolean mirrored) throws IOException {
		FileUtils.forceMkdir(targetDirectory.getParentFile());
		FileUtils.deleteQuietly(targetDirectory);

		log.debug("Cloning {} into {}", repositoryUrl, targetDirectory);
		List<String> commandLine = Lists.newArrayList("git", "clone", repositoryUrl, targetDirectory.getPath());
		if (mirrored) {
			commandLine.add("--mirror");
		}
		ProcessUtils.executeIn(null, commandLine);
	}

	@Override
	public void update(File targetDirectory, boolean mirrored) throws IOException {
		List<String> fetchCommand = Lists.newArrayList("git", "fetch");

		// Cached repositories need to update all branches
		if (mirrored) {
			fetchCommand.add("--all");
		}
		ProcessUtils.executeIn(targetDirectory, fetchCommand);

		// Update working copy unless this is a cached clone
		if (!mirrored) {
			String updateCommand = configuration.getString(GIT_UPDATE, "git rebase --autostash");
			ProcessUtils.executeIn(targetDirectory, Lists.newArrayList(updateCommand.split(" ")));
		}
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public boolean hasChanges(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "status", "--branch", "--porcelain"), false, false);

		// ## master...origin/master [ahead 1]
		//  M non-added-modification.txt
		// M  added-modification.txt
		// ?? non-added-file.txt

		boolean hasChanges = CharStreams.readLines(new InputStreamReader(process.getInputStream(), Charsets.UTF_8), new LineProcessor<Boolean>() {
			boolean hasChanges = false;

			@Override
			@SuppressWarnings("NullableProblems")
			public boolean processLine(String line) throws IOException {
				if (line.startsWith("#")) {
					// Check if we have commits to be pushed
					hasChanges = STATUS_AHEAD.matcher(line).matches();
				} else if (!line.isEmpty()) {
					// Check if we have uncommitted files
					hasChanges = true;
				}
				return !hasChanges;
			}

			@Override
			public Boolean getResult() {
				return hasChanges;
			}
		});
		return hasChanges;
	}

	@Override
	public void activate(String repositoryUrl, File targetDirectory) throws IOException {
		ProcessUtils.executeIn(targetDirectory, Lists.newArrayList("git", "remote", "set-url", "origin", repositoryUrl));
	}

	@Override
	public boolean isMirroringSupported() {
		return true;
	}

	@Override
	public String normalizeRepositoryUrl(String repositoryUrl) {
		return repositoryUrl.replaceAll("(\\.git)|/$", "");
	}

	@Override
	public String resolveRepositoryName(String repository) {
		Matcher matcher = REPOSITORY_URL_PATTERN.matcher(repository);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}
}
