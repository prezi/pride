package com.prezi.pride.cli.ivyversions;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class ResolverStrategy {
	private final List<VersionMatcher> matchers;

	public ResolverStrategy() {
		ImmutableList.Builder<VersionMatcher> matcherBuilder = ImmutableList.builder();
		matcherBuilder.add(new VersionRangeMatcher(new ExactVersionMatcher()));
		matcherBuilder.add(new SubVersionMatcher(new ExactVersionMatcher()));
		matcherBuilder.add(new LatestVersionMatcher());
		matcherBuilder.add(new ExactVersionMatcher());
		this.matchers = matcherBuilder.build();
	}

	public boolean accept(String selector, String candidate) {
		for (VersionMatcher matcher : matchers) {
			if (matcher.canHandle(selector)) {
				return matcher.accept(selector, candidate);
			}
		}
		throw new IllegalArgumentException("Invalid version selector: " + selector);
	}
}
