package com.prezi.gradle.pride.filters;

import com.google.common.base.Joiner;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public final class Filters {

	public static Filter all() {
		return new Filter() {
			@Override
			public boolean matches(Pride pride, Module module) throws IOException {
				return true;
			}

			@Override
			public String toString() {
				return "ALL";
			}
		};
	}

	public static Filter none() {
		return new Filter() {
			@Override
			public boolean matches(Pride pride, Module module) throws IOException {
				return false;
			}

			@Override
			public String toString() {
				return "NONE";
			}
		};
	}

	public static Filter not(final Filter filter) {
		return new Filter() {
			@Override
			public boolean matches(Pride pride, Module module) throws IOException {
				return !filter.matches(pride, module);
			}

			@Override
			public String toString() {
				return "NOT (" + filter + ")";
			}
		};
	}

	public static Filter and(Filter... filters) {
		return and(Arrays.asList(filters));
	}
	public static Filter and(final Collection<Filter> filters) {
		if (filters.isEmpty()) {
			return all();
		}
		return new Filter() {
			@Override
			public boolean matches(Pride pride, Module module) throws IOException {
				for (Filter filter : filters) {
					if (!filter.matches(pride, module)) {
						return false;
					}
				}
				return true;
			}

			@Override
			public String toString() {
				return "(" + Joiner.on(") AND (").join(filters) + ")";
			}
		};
	}

	public static Filter or(Filter... filters) {
		return or(Arrays.asList(filters));
	}
	public static Filter or(final Collection<Filter> filters) {
		if (filters.isEmpty()) {
			return all();
		}
		return new Filter() {
			@Override
			public boolean matches(Pride pride, Module module) throws IOException {
				boolean matches = true;
				for (Filter filter : filters) {
					if (filter.matches(pride, module)) {
						return true;
					}
					matches = false;
				}
				return matches;
			}

			@Override
			public String toString() {
				return "(" + Joiner.on(") OR (").join(filters) + ")";
			}
		};
	}
}
