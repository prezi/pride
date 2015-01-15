package com.prezi.pride.cli;

import com.prezi.pride.Named;
import com.prezi.pride.vcs.Vcs;

/**
 * Created by lptr on 04/11/14.
 */
public class ExportedModule implements Named {
	private final String module;
	private final String remote;
	private final String revision;
	private final Vcs vcs;

	public ExportedModule(String module, String remote, String revision, Vcs vcs) {
		this.module = module;
		this.remote = remote;
		this.revision = revision;
		this.vcs = vcs;
	}

	@Override
	public String getName() {
		return module;
	}

	public String getModule() {
		return module;
	}

	public String getRemote() {
		return remote;
	}

	public String getRevision() {
		return revision;
	}

	public Vcs getVcs() {
		return vcs;
	}

	@Override
	public String toString() {
		return module;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ExportedModule)) return false;
		ExportedModule that = (ExportedModule) o;
		return module.equals(that.module);
	}

	@Override
	public int hashCode() {
		return module.hashCode();
	}
}
