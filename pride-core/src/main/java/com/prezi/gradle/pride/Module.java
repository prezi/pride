package com.prezi.gradle.pride;

import com.prezi.gradle.pride.vcs.Vcs;

public class Module implements Named, Comparable<Module> {
	private final String name;
	private final String remote;
	private final String branch;
	private final Vcs vcs;

	public Module(String name, String remote, String branch, Vcs vcs) {
		this.name = name;
		this.remote = remote;
		this.branch = branch;
		this.vcs = vcs;
	}

	@Override
	public int compareTo(Module o) {
		return name.compareTo(o.name);
	}

	@Override
	public String getName() {
		return name;
	}

	public String getRemote() {
		return remote;
	}

	public String getBranch() {
		return branch;
	}

	public Vcs getVcs() {
		return vcs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Module module = (Module) o;
		return name.equals(module.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}
