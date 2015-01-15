package com.prezi.pride.vcs.svn;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.prezi.pride.ProcessUtils;
import com.prezi.pride.vcs.VcsStatus;
import com.prezi.pride.vcs.VcsSupport;
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

	private static final Pattern REPOSITORY_NAME_PATTERN = Pattern.compile("^"
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
	public void checkout(String repositoryUrl, File targetDirectory, String revision, boolean recursive, boolean mirrored) throws IOException {
		FileUtils.forceMkdir(targetDirectory.getParentFile());
		FileUtils.deleteQuietly(targetDirectory);

		String branchUrl = new RepositoryUrl(repositoryUrl, revision).toUrl();

		log.debug("Checking out {} into {}", branchUrl, targetDirectory);
		ImmutableList.Builder<String> checkoutCommand = ImmutableList.<String> builder().add("svn").add("checkout");
		if (!recursive) {
			checkoutCommand.add("--ignore-externals");
		}
		checkoutCommand.add(branchUrl).add(targetDirectory.getPath());
		ProcessUtils.executeIn(null, checkoutCommand.build());
	}

	@Override
	public void update(File targetDirectory, String revision, boolean recursive, boolean mirrored) throws IOException {
		ImmutableList.Builder<String> updateCommand = ImmutableList.builder();
		if (!Strings.isNullOrEmpty(revision) && !revision.equals(getBranch(targetDirectory))) {
			updateCommand.add("svn", "switch", getRepositoryUrl(targetDirectory) + "/" + revision, ".");
		} else {
			updateCommand.add("svn", "update");
		}
		if (!recursive) {
			updateCommand.add("--ignore-externals");
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
	public void activate(String repositoryUrl, File targetDirectory) throws IOException {
		throw new AssertionError("Cannot activate an SVN repository");
	}

	@Override
	public boolean isMirroringSupported() {
		return false;
	}

	@Override
	public String getRepositoryUrl(File targetDirectory) throws IOException {
		return getRepositoryUrlInternal(targetDirectory).root;
	}

	@Override
	public String getBranch(File targetDirectory) throws IOException {
		return getRepositoryUrlInternal(targetDirectory).branch;
	}

	@Override
	public String getDefaultBranch() throws IOException {
		return "trunk";
	}

	private RepositoryUrl getRepositoryUrlInternal(File targetDirectory) throws IOException {
		String fullUrl = getInfoValue(targetDirectory, ROOT_URL);
		return RepositoryUrl.fromString(fullUrl);
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
		Matcher matcher = REPOSITORY_NAME_PATTERN.matcher(repository);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}

	private static class RepositoryUrl {
		public static final String TRUNK = "trunk";
		private static Pattern URL_PATTERN = Pattern.compile("(.*)/(?:trunk|branches/([^/]+))/?");

		public final String root;
		public final String branch;

		private RepositoryUrl(String root, String branch) {
			this.root = root;
			this.branch = Strings.isNullOrEmpty(branch) ? TRUNK : branch;
		}

		public static RepositoryUrl fromString(String url) {
			Matcher matcher = URL_PATTERN.matcher(url);
			if (matcher.matches()) {
				return new RepositoryUrl(matcher.group(1), matcher.group(2));
			}
			throw new IllegalArgumentException("Unable to parse URL: " + url);
		}

		public String toUrl() {
			if (TRUNK.equals(branch)) {
				return root + "/" + TRUNK;
			} else {
				return root + "/branches/" + branch;
			}
		}
	}
}
