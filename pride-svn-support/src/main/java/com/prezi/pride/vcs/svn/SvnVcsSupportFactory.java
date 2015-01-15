package com.prezi.pride.vcs.svn;

import com.prezi.pride.vcs.VcsSupport;
import com.prezi.pride.vcs.VcsSupportFactory;
import org.apache.commons.configuration.Configuration;

import java.io.File;

public class SvnVcsSupportFactory implements VcsSupportFactory {
	@Override
	public String getType() {
		return "svn";
	}

	@Override
	public VcsSupport createVcsSupport(Configuration configuration) {
		return new SvnVcsSupport();
	}

	@Override
	public boolean canSupport(File targetDirectory) {
		return new File(targetDirectory, ".svn").isDirectory();
	}
}
