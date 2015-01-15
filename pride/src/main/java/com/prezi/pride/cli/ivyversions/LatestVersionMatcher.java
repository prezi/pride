package com.prezi.pride.cli.ivyversions;

public class LatestVersionMatcher implements VersionMatcher {
    public boolean canHandle(String selector) {
        return selector.startsWith("latest.");
    }

	@Override
    public boolean accept(String selector, String candidate) {
        return true;
    }

	@Override
    public int compare(String selector, String candidate) {
        return 0;
    }
}
