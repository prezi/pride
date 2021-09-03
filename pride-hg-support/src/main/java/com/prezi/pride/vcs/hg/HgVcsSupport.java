package com.prezi.pride.vcs.hg;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import com.prezi.pride.ProcessUtils;
import com.prezi.pride.PrideException;
import com.prezi.pride.vcs.VcsStatus;
import com.prezi.pride.vcs.VcsSupport;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HgVcsSupport implements VcsSupport {
	private static final Logger log = LoggerFactory.getLogger(HgVcsSupport.class);
	private static final Pattern REPOSITORY_URL_PATTERN = Pattern.compile("^"
				+ "(?:"
					+ "\\w+@.+:(?:.+/)?"			//	SCP-like pattern prefix
				+ "|"
					+ "(?:https?|ftps?|rsync|ssh)://"	// Protocol prefix
					+ ".+/"										// path to repo
				+ ")"
				+ "(.+?)"										// repo name
				+ "/?"											// optional trailing slash
				+ "$", Pattern.COMMENTS);
	private static final Pattern REMOTE_LINE = Pattern.compile("(\\S+)\\s+=\\s+(\\S+)");

	private final Configuration configuration;

	HgVcsSupport(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void checkout(String repositoryUrl, File targetDirectory, String revision, boolean recursive, boolean mirrored) throws IOException {
		FileUtils.forceMkdir(targetDirectory.getParentFile());
		FileUtils.deleteQuietly(targetDirectory);

		log.debug("Cloning {} into {}", repositoryUrl, targetDirectory);
		ImmutableList.Builder<String> cloneCommandLine = ImmutableList.builder();
		cloneCommandLine.add("hg", "clone", repositoryUrl, targetDirectory.getPath());

		if (mirrored) {
			cloneCommandLine.add("--noupdate");
		}

		ProcessUtils.executeIn(null, cloneCommandLine.build());

		if (!Strings.isNullOrEmpty(revision)) {
			ImmutableList.Builder<String> checkoutCommandLine = ImmutableList.builder();
			checkoutCommandLine.add("hg", "update", revision);
			ProcessUtils.executeIn(targetDirectory, checkoutCommandLine.build());
		}
	}

	@Override
	public void update(File targetDirectory, String revision, boolean recursive, boolean mirrored) throws IOException {
		if (!mirrored) {
			if (hasUnpublishedChanges(targetDirectory)) {
				ProcessUtils.executeIn(targetDirectory, Arrays.asList("hg", "--config", "extensions.rebase=", "pull", "--rebase", "--tool", "internal:fail"));
			} else {
				ProcessUtils.executeIn(targetDirectory, Arrays.asList("hg", "pull", "--update"));
			}

			if (!Strings.isNullOrEmpty(revision) && !revision.equals(getBranch(targetDirectory))) {
				ProcessUtils.executeIn(targetDirectory, Arrays.asList("hg", "checkout", revision));
			}
		} else {
			ProcessUtils.executeIn(targetDirectory, Arrays.asList("hg", "pull"));
		}
	}

	@Override
	public boolean hasChanges(File targetDirectory) throws IOException {
		VcsStatus.Builder builder = VcsStatus.builder("");
		getStatusInternal(targetDirectory, builder);
		VcsStatus status = builder.build();
		return status.hasUncommittedChanges() || status.hasUnpublishedChanges();
	}

	@Override
	public VcsStatus getStatus(File targetDirectory) throws IOException {
		final VcsStatus.Builder status = VcsStatus.builder(getRevision(targetDirectory));
		status.withBranch(getBranch(targetDirectory));
		getStatusInternal(targetDirectory, status);
		return status.build();
	}

	private void getStatusInternal(File targetDirectory, final VcsStatus.Builder status) throws IOException {
		checkModified(targetDirectory, status);
		checkOutgoing(targetDirectory, status);
	}

	private void checkModified(File targetDirectory, final VcsStatus.Builder status) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("hg", "status"), false, false);
		String result = new String(ByteStreams.toByteArray(process.getInputStream()), Charsets.UTF_8);
		status.withUncommittedChanges(!result.isEmpty());
	}

	private void checkOutgoing(File targetDirectory, final VcsStatus.Builder status) throws IOException {
		boolean unpublishedChanges = hasUnpublishedChanges(targetDirectory);
		status.withUnpublishedChanges(unpublishedChanges);
	}

	private boolean hasUnpublishedChanges(File targetDirectory) throws IOException {
		int exitStatusOk = 0;
		int exitStatusWhenThereAreNoOutgoingChanges = 1;
		int exitStatusWhenOriginNoLongerExists = 255;
		List<Integer> acceptableExitCodes = Arrays.asList(
				exitStatusOk,
				exitStatusWhenThereAreNoOutgoingChanges,
				exitStatusWhenOriginNoLongerExists
		);

		List<String> commandLine = Arrays.asList("hg", "outgoing", "--quiet", "--template", "{node}\\n");
		Process process = ProcessUtils.executeIn(targetDirectory, commandLine, false, false, acceptableExitCodes);
		String result = new String(ByteStreams.toByteArray(process.getInputStream()), Charsets.UTF_8);
		return !result.isEmpty();
	}

	private String getRevision(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("hg", "id", "--id"), false, false);
		String result = new String(ByteStreams.toByteArray(process.getInputStream()), Charsets.UTF_8);
		return result.trim().replace("+", "");
	}

	@Override
	public String getBranch(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("hg", "branch"), false, false);
		String result = new String(ByteStreams.toByteArray(process.getInputStream()), Charsets.UTF_8);
		return result.trim();
	}

	@Override
	public String getDefaultBranch() throws IOException {
		return "default";
	}

	@Override
	public void activate(String repositoryUrl, File targetDirectory) throws IOException {
		log.debug("Activating '{}' with url '{}'", targetDirectory, repositoryUrl);
		File hgrcFile = new File(targetDirectory, ".hg/hgrc");
		String hgrcText = FileUtils.readFileToString(hgrcFile, Charsets.UTF_8);
		String activated = hgrcText.replaceAll("default = .*", "default = " + repositoryUrl);
		FileUtils.write(hgrcFile, activated, Charsets.UTF_8);
	}

	@Override
	public boolean isMirroringSupported() {
		return true;
	}

	@Override
	public String getRepositoryUrl(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("hg", "paths"), false, false);
		List<String> remoteLines = CharStreams.readLines(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
		Map<String, String> remoteUrls = Maps.newLinkedHashMap();
		for (String remoteLine : remoteLines) {
			Matcher matcher = REMOTE_LINE.matcher(remoteLine);
			if (!matcher.matches()) {
				continue;
			}
			String remote = matcher.group(1);
			String url = matcher.group(2);
			remoteUrls.put(remote, url);
		}

		if (remoteUrls.size() == 0) {
			// We found no remotes
			return null;
		} else if (remoteUrls.size() == 1) {
			// There's only one, use it
			return Iterables.getLast(remoteUrls.values());
		} else {
			String defaultRemote = remoteUrls.get("default");
			if (defaultRemote != null) {
				// There's one called 'default', use that
				return defaultRemote;
			} else {
				// Use the first one, for better or worse
				return Iterables.getFirst(remoteUrls.values(), null);
			}
		}
	}

	@Override
	public String normalizeRepositoryUrl(String repositoryUrl) {
		return repositoryUrl.replaceAll("/$", "");
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
