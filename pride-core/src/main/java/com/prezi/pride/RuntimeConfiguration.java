package com.prezi.pride;

import org.apache.commons.configuration.Configuration;

/**
 * A configuration stack that contains defaults and overrides alongside the loaded file configurations.
 *
 * <p>The goal of this class is to maintain a stack of configurations from most-local to most-global.
 * The list of configuration is built as follows, in query-order:
 * <ul>
 *     <li>overrides</li>
 *     <li>local configuration</li>
 *     <li>...</li>
 *     <li>global configuration</li>
 *     <li>defaults</li>
 * </ul>
 */
public interface RuntimeConfiguration extends Configuration {
	RuntimeConfiguration withConfiguration(Configuration configuration);

	boolean override(String property, Boolean override);

	boolean override(String property, boolean overrideEnabled, boolean overrideDisabled);

	String override(String property, String override);
}
