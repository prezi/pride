package com.prezi.pride.vcs;

public class Vcs {
	private final String type;
	private final VcsSupport support;

	public Vcs(String type, VcsSupport support) {
		this.type = type;
		this.support = support;
	}

	public String getType() {
		return type;
	}

	public VcsSupport getSupport() {
		return support;
	}

}
