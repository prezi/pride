package com.prezi.gradle.pride.vcs.git;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import com.prezi.gradle.pride.ProcessUtils;
import com.prezi.gradle.pride.vcs.VcsStatus;
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
import java.util.Map;
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
	private static final Pattern REMOTE_LINE = Pattern.compile("(\\S+)\\s+(\\S+)\\s+\\((\\S+)\\)");

	private final Configuration configuration;

	GitVcsSupport(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void checkout(String repositoryUrl, File targetDirectory, String branch, boolean recursive, boolean mirrored) throws IOException {
		FileUtils.forceMkdir(targetDirectory.getParentFile());
		FileUtils.deleteQuietly(targetDirectory);

		log.debug("Cloning {} into {}", repositoryUrl, targetDirectory);
		ImmutableList.Builder<String> commandLine = ImmutableList.builder();
		commandLine.add("git", "clone", repositoryUrl, targetDirectory.getPath());

		if (!Strings.isNullOrEmpty(branch)) {
			commandLine.add("--branch", branch);
		}

		if (mirrored) {
			commandLine.add("--mirror");
		} else if (recursive) {
			commandLine.add("--recursive");
		}
		ProcessUtils.executeIn(null, commandLine.build());
	}

	@Override
	public void update(File targetDirectory, String branch, boolean recursive, boolean mirrored) throws IOException {
		// Cached repositories need to update all branches
		if (!mirrored) {
			// Fetch changes
			ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "remote", "update"));

			if (!Strings.isNullOrEmpty(branch) && !branch.equals(getBranch(targetDirectory))) {
				ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "checkout", branch));
			} else {
				// Update working copy
				String updateCommand = configuration.getString(GIT_UPDATE, "git rebase --autostash");
				ProcessUtils.executeIn(targetDirectory, Arrays.asList((String[]) updateCommand.split(" ")));
			}

			// Update submodules if necessary
			if (recursive) {
				ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "submodule", "update", "--init", "--recursive"));
			}
		} else {
			// Update the remote
			ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "remote", "update", "--prune"));
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
		// ## master...origin/master [ahead 1]
		//  M non-added-modification.txt
		// M  added-modification.txt
		// ?? non-added-file.txt
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "status", "--branch", "--porcelain"), false, false);

		CharStreams.readLines(new InputStreamReader(process.getInputStream(), Charsets.UTF_8), new LineProcessor<Void>() {
			@Override
			@SuppressWarnings("NullableProblems")
			public boolean processLine(String line) throws IOException {
				if (line.startsWith("#")) {
					// Check if we have commits to be pushed
					if (STATUS_AHEAD.matcher(line).matches()) {
						status.withUnpublishedChanges(true);
					}
				} else if (!line.isEmpty()) {
					// Check if we have uncommitted files
					status.withUncommittedChanges(true);
					return false;
				}
				return true;
			}

			@Override
			public Void getResult() {
				return null;
			}
		});
	}

	private String getRevision(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "rev-parse", "HEAD"), false, false);
		String result = new String(ByteStreams.toByteArray(process.getInputStream()), Charsets.UTF_8);
		return result.trim().substring(0, 7);
	}

	@Override
	public String getBranch(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "branch", "--list"), false, false);
		return CharStreams.readLines(new InputStreamReader(process.getInputStream(), Charsets.UTF_8), new LineProcessor<String>() {
			private String branch;

			@Override
			@SuppressWarnings("NullableProblems")
			public boolean processLine(String line) throws IOException {
				if (line.startsWith("* ")) {
					String branchCandidate = line.substring(2);
					if (!branchCandidate.startsWith("(detached from ")) {
						this.branch = branchCandidate;
						return false;
					}
				}
				return true;
			}

			@Override
			public String getResult() {
				return branch;
			}
		});
	}

	@Override
	public String getDefaultBranch() throws IOException {
		return "master";
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
	public String getRepositoryUrl(File targetDirectory) throws IOException {
		Process process = ProcessUtils.executeIn(targetDirectory, Arrays.asList("git", "remote", "-v"), false, false);
		List<String> remoteLines = CharStreams.readLines(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
		Map<String, String> remoteUrls = Maps.newLinkedHashMap();
		for (String remoteLine : remoteLines) {
			Matcher matcher = REMOTE_LINE.matcher(remoteLine);
			if (!matcher.matches()) {
				continue;
			}
			if (!"fetch".equals(matcher.group(3))) {
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
			String origin = remoteUrls.get("origin");
			if (origin != null) {
				// There's one called 'origin', use that
				return origin;
			} else {
				// Use the first one, for better or worse
				return Iterables.getFirst(remoteUrls.values(), null);
			}
		}
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
