package com.prezi.gradle.pride.vcs.git;

import com.prezi.gradle.pride.ProcessUtils;
import com.prezi.gradle.pride.vcs.VcsSupport;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitVcsSupport implements VcsSupport {
	public static final String GIT_UPDATE = "git.update";

	private static final Logger log = LoggerFactory.getLogger(GitVcsSupport.class);
	private final Configuration configuration;

	GitVcsSupport(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void checkout(String repositoryUrl, File targetDirectory, boolean mirrored) throws IOException {
		FileUtils.forceMkdir(targetDirectory.getParentFile());
		FileUtils.deleteDirectory(targetDirectory);

		log.debug("Cloning {} into {}", repositoryUrl, targetDirectory);
		List<String> commandLine = Arrays.asList("git", "clone", repositoryUrl, targetDirectory.toString());
		if (mirrored) {
			commandLine.add("--mirror");
		}
		ProcessUtils.executeIn(null, commandLine);
	}

	@Override
	public void update(File targetDirectory, boolean mirrored) throws IOException {
		List<String> fetchCommand = Arrays.asList("git", "fetch");

		// Cached repositories need to update all branches
		if (mirrored) {
			fetchCommand.add("--all");
		}
		ProcessUtils.executeIn(targetDirectory, fetchCommand);

		// Update working copy unless this is a cached clone
		if (!mirrored) {
			String updateCommand = configuration.getString(GIT_UPDATE, "git rebase --autostash");
			ProcessUtils.executeIn(targetDirectory, Arrays.asList(updateCommand.split(" ")));
		}
	}

	@Override
	public void activate(String repositoryUrl, File targetDirectory) throws IOException {
		ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "remote", "set-url", "origin", repositoryUrl));
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
	public String resolveRepositoryName(String repositoryUrl) {
		// TODO Make this static
		Pattern pattern = Pattern.compile("^"
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
		Matcher matcher = pattern.matcher(repositoryUrl);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}
}
