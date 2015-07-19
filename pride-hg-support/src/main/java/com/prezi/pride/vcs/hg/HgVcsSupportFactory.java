package com.prezi.pride.vcs.hg;

import com.prezi.pride.vcs.VcsSupport;
import com.prezi.pride.vcs.VcsSupportFactory;
import org.apache.commons.configuration.Configuration;

import java.io.File;

public class HgVcsSupportFactory implements VcsSupportFactory {
	@Override
	public String getType() {
		return "hg";
	}

	@Override
	public VcsSupport createVcsSupport(Configuration configuration) {
		return new HgVcsSupport(configuration);
	}

	@Override
	public boolean canSupport(File targetDirectory) {
		return new File(targetDirectory, ".hg").isDirectory();
	}
}
