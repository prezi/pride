package com.prezi.gradle.pride;

import com.prezi.gradle.pride.vcs.Vcs;

public class Module implements Comparable<Module> {
	private final String name;
	private final Vcs vcs;

	public Module(String name, Vcs vcs) {
		this.name = name;
		this.vcs = vcs;
	}

	@Override
	public int compareTo(Module o) {
		return name.compareTo(o.name);
	}

	public String getName() {
		return name;
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
