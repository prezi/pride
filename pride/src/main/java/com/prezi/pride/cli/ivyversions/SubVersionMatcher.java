package com.prezi.pride.cli.ivyversions;

import java.util.Comparator;

/**
 * Version matcher for dynamic version selectors ending in '+'.
 */
public class SubVersionMatcher implements VersionMatcher {
	private final Comparator<String> staticVersionComparator;

	public SubVersionMatcher(VersionMatcher staticVersionComparator) {
		this.staticVersionComparator = staticVersionComparator;
	}

	public boolean canHandle(String selector) {
		return selector.endsWith("+");
	}

	public boolean accept(String selector, String candidate) {
		String prefix = selector.substring(0, selector.length() - 1);
		return candidate.startsWith(prefix);
	}

	public int compare(String selector, String candidate) {
		if (accept(selector, candidate)) {
			return 1;
		}
		return staticVersionComparator.compare(selector, candidate);
	}
}
