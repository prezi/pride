package com.prezi.gradle.pride;

import com.prezi.gradle.pride.vcs.Vcs;

public class Module implements Comparable<Module> {
	public final String name;
	public final Vcs vcs;

	public Module(String name, Vcs vcs) {
		this.name = name;
		this.vcs = vcs;
	}

	@Override
	public int compareTo(Module o) {
		return name.compareTo(o.name);
	}
}
