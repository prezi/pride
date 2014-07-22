package com.prezi.gradle.pride.vcs.git;

import com.prezi.gradle.pride.vcs.VcsSupport;
import com.prezi.gradle.pride.vcs.VcsSupportFactory;
import org.apache.commons.configuration.Configuration;

import java.io.File;

public class GitVcsSupportFactory implements VcsSupportFactory {
	@Override
	public String getType() {
		return "git";
	}

	@Override
	public VcsSupport createVcsSupport(Configuration configuration) {
		return new GitVcsSupport(configuration);
	}

	@Override
	public boolean canSupport(File targetDirectory) {
		return new File(targetDirectory, ".git").isDirectory();
	}
}
