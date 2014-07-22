package com.prezi.gradle.pride.vcs.svn;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.prezi.gradle.pride.ProcessUtils;
import com.prezi.gradle.pride.vcs.VcsSupport;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SvnVcsSupport implements VcsSupport {

	private static final Pattern REPOSITORY_URL_PATTERN = Pattern.compile("^"
				+ "(?:svn|https?)://"						// Protocol prefix
				+ ".+/"										// path to repo
				+ "(.+?)" 									// repo name
				+ "(?:\\.git)?"								// optional .git suffix
				+ "/?"										// optional trailing slash
				+ "$", Pattern.COMMENTS);

	private static final Logger log = LoggerFactory.getLogger(SvnVcsSupport.class);

	@Override
	public void checkout(String repositoryUrl, File targetDirectory, boolean mirrored) throws IOException {
		FileUtils.forceMkdir(targetDirectory.getParentFile());
		FileUtils.deleteQuietly(targetDirectory);

		String trunkUrl = repositoryUrl;
		if (!trunkUrl.endsWith("/")) {
			trunkUrl += "/";
		}
		trunkUrl += "trunk";

		log.debug("Checking out {} into {}", trunkUrl, targetDirectory);
		ProcessUtils.executeIn(null, Arrays.asList("svn", "checkout", trunkUrl, targetDirectory.getPath()));
	}

	@Override
	public void update(File targetDirectory, boolean mirrored) throws IOException {
		ProcessUtils.executeIn(targetDirectory, Arrays.asList("svn", "update"));
	}

	@Override
	public boolean hasChanges(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("svn", "status"), false, false);
		String result = new String(ByteStreams.toByteArray(process.getInputStream()), Charsets.UTF_8);
		return !result.trim().isEmpty();
	}

	@Override
	public void activate(String repositoryUrl, File targetDirectory) throws IOException {
		throw new AssertionError("Cannot activate an SVN repository");
	}

	@Override
	public boolean isMirroringSupported() {
		return false;
	}

	@Override
	public String normalizeRepositoryUrl(String repositoryUrl) {
		return repositoryUrl;
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
