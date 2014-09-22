package com.prezi.gradle.pride.vcs.svn;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.prezi.gradle.pride.ProcessUtils;
import com.prezi.gradle.pride.vcs.VcsStatus;
import com.prezi.gradle.pride.vcs.VcsSupport;
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

public class SvnVcsSupport implements VcsSupport {

	private static final Pattern REPOSITORY_URL_PATTERN = Pattern.compile("^"
				+ "(?:svn|https?)://"						// Protocol prefix
				+ ".+/"										// path to repo
				+ "(.+?)" 									// repo name
				+ "(?:\\.git)?"								// optional .git suffix
				+ "/?"										// optional trailing slash
				+ "$", Pattern.COMMENTS);
	private static final Pattern REVISION = Pattern.compile("Revision: (.*)");
	private static final Pattern ROOT_URL = Pattern.compile("Repository Root: (.*)");

	private static final Logger log = LoggerFactory.getLogger(SvnVcsSupport.class);

	@Override
	public void checkout(String repositoryUrl, File targetDirectory, String branch, boolean recursive, boolean mirrored) throws IOException {
		FileUtils.forceMkdir(targetDirectory.getParentFile());
		FileUtils.deleteQuietly(targetDirectory);

		String trunkUrl = repositoryUrl;
		if (!trunkUrl.endsWith("/")) {
			trunkUrl += "/";
		}

		if (!Strings.isNullOrEmpty(branch)) {
			trunkUrl += branch;
		} else {
			trunkUrl += "trunk";
		}

		log.debug("Checking out {} into {}", trunkUrl, targetDirectory);
		ImmutableList.Builder<String> checkoutCommand = ImmutableList.<String> builder().add("svn").add("checkout");
		if (!recursive) {
			checkoutCommand.add("--depth=files");
		}
		checkoutCommand.add(trunkUrl).add(targetDirectory.getPath());
		ProcessUtils.executeIn(null, checkoutCommand.build());
	}

	@Override
	public void update(File targetDirectory, boolean recursive, boolean mirrored) throws IOException {
		ImmutableList.Builder<String> updateCommand = ImmutableList.<String> builder().add("svn").add("update");
		if (!recursive) {
			updateCommand.add("--depth=files");
		}
		ProcessUtils.executeIn(targetDirectory, updateCommand.build());
	}

	@Override
	public boolean hasChanges(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("svn", "status"), false, false);
		String result = new String(ByteStreams.toByteArray(process.getInputStream()), Charsets.UTF_8);
		return !result.trim().isEmpty();
	}

	@Override
	public VcsStatus getStatus(File targetDirectory) throws IOException {
		VcsStatus.Builder status = VcsStatus.builder(getRevision(targetDirectory));
		status.withUncommittedChanges(hasChanges(targetDirectory));
		status.withBranch(getBranch(targetDirectory));
		return status.build();
	}

	private String getRevision(File targetDirectory) throws IOException {
		return getInfoValue(targetDirectory, REVISION);
	}

	@Override
	public String getBranch(File targetDirectory) throws IOException {
		String repositoryUrl = getRepositoryUrl(targetDirectory);
		return repositoryUrl.substring(repositoryUrl.lastIndexOf('/') + 1);
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
	public String getRepositoryUrl(File targetDirectory) throws IOException {
		return getInfoValue(targetDirectory, ROOT_URL);
	}

	private String getInfoValue(File targetDirectory, Pattern pattern) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("svn", "info"), false, false);
		List<String> infoLines = CharStreams.readLines(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
		for (String remoteLine : infoLines) {
			Matcher matcher = pattern.matcher(remoteLine);
			if (!matcher.matches()) {
				continue;
			}
			return matcher.group(1);
		}
		return null;
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
