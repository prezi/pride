package com.prezi.gradle.pride.vcs.file;

import com.prezi.gradle.pride.vcs.VcsStatus;
import com.prezi.gradle.pride.vcs.VcsSupport;
import com.prezi.gradle.pride.vcs.VcsSupportFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileVcsSupportFactory implements VcsSupportFactory {
	@Override
	public String getType() {
		return "file";
	}

	@Override
	public VcsSupport createVcsSupport(Configuration configuration) {
		return new FileVcsSupport();
	}

	@Override
	public boolean canSupport(File targetDirectory) {
		return true;
	}

	private static class FileVcsSupport implements VcsSupport {
		@Override
		public void checkout(String repositoryUrl, File targetDirectory, String branch, boolean recursive, boolean mirrored) throws IOException {
			File sourceDirectory = new File(repositoryUrl);
			if (!sourceDirectory.exists()) {
				throw new FileNotFoundException("Cannot find " + sourceDirectory);
			}
			FileUtils.copyDirectory(sourceDirectory, targetDirectory);
		}

		@Override
		public void update(File targetDirectory, String branch, boolean recursive, boolean mirrored) throws IOException {
			// Do nothing
		}

		@Override
		public boolean hasChanges(File targetDirectory) throws IOException {
			return false;
		}

		@Override
		public VcsStatus getStatus(File targetDirectory) throws IOException {
			return VcsStatus.builder("none").build();
		}

		@Override
		public void activate(String repositoryUrl, File targetDirectory) throws IOException {
			// Do nothing
		}

		@Override
		public boolean isMirroringSupported() {
			return false;
		}

		@Override
		public String getRepositoryUrl(File targetDirectory) throws IOException {
			return targetDirectory.getAbsolutePath();
		}

		@Override
		public String getBranch(File targetDirectory) throws IOException {
			return null;
		}

		@Override
		public String normalizeRepositoryUrl(String repositoryUrl) {
			return repositoryUrl;
		}

		@Override
		public String resolveRepositoryName(String repository) {
			return new File(repository).getName();
		}
	}
}
