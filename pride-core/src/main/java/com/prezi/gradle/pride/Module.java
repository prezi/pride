package com.prezi.gradle.pride;

import com.prezi.gradle.pride.vcs.Vcs;

public class Module implements Comparable<Module> {
	private final String name;
	private final String remote;
	private final Vcs vcs;

	public Module(String name, String remote, Vcs vcs) {
		this.name = name;
		this.remote = remote;
		this.vcs = vcs;
	}

	@Override
	public int compareTo(Module o) {
		return name.compareTo(o.name);
	}

	public String getName() {
		return name;
	}

	public String getRemote() {
		return remote;
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
}
