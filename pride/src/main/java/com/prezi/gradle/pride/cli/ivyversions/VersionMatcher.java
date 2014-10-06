package com.prezi.gradle.pride.cli.ivyversions;

import java.util.Comparator;

/**
 * Compares version selectors against candidate versions, indicating whether they match or not.
 * <p>This interface was initially derived from {@code org.apache.ivy.plugins.version.VersionMatcher}.
 */
public interface VersionMatcher extends Comparator<String> {
	public boolean canHandle(String selector);

	/**
	 * Indicates if the given version selector matches the given candidate version.
	 */
	public boolean accept(String selector, String candidate);

	/**
	 * Compares a version selector with a candidate version to indicate which is greater. If there is
	 * not enough information to tell which is greater, the version selector should be considered greater
	 * and this method should return 0.
	 */
	public int compare(String selector, String candidate);
}
