package com.prezi.gradle.pride.filters;

import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;

import java.util.regex.Pattern;

public class NameMatcherFilter implements Filter {
	private final Pattern pattern;

	public NameMatcherFilter(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override
	public boolean matches(Pride pride, Module module) {
		return pattern.matcher(module.getName()).matches();
	}

	@Override
	public String toString() {
		return "NAME = /" + pattern.pattern() + "/";
	}
}
