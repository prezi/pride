package com.prezi.pride.cli.commands;

import com.google.common.base.Function;
import com.prezi.pride.Module;
import com.prezi.pride.Pride;
import com.prezi.pride.filters.BranchFilter;
import com.prezi.pride.filters.ChangedFilter;
import com.prezi.pride.filters.Filter;
import com.prezi.pride.filters.Filters;
import com.prezi.pride.filters.NameMatcherFilter;
import io.airlift.command.Option;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.collect.Collections2.transform;
import static com.prezi.pride.filters.Filters.and;
import static com.prezi.pride.filters.Filters.or;

public abstract class AbstractFilteredPrideCommand extends AbstractPrideCommand {
	@Option(name = "--exclude",
			title = "regex",
			description = "Do not execute command on module (can be specified multiple times)")
	private List<String> excludeModules;

	@Option(name = {"-b", "--branch"},
			title = "regex",
			description = "Execute only on modules that are on a matching branch (can be specified multiple times)")
	private List<String> branches;

	@Option(name = {"-c", "--changed"},
			description = "Execute only on modules that have changes (uncommitted or unpublished)")
	private boolean changes;

	@Option(name = {"-u", "--uncommitted"},
			description = "Execute only on modules that have uncommitted changes")
	private boolean changesUncommitted;

	@Option(name = {"-U", "--unpublished"},
			description = "Execute only on modules that have unpublished changes")
	private boolean changesUnpublished;

	@Override
	public final void executeInPride(Pride pride) throws Exception {
		Collection<String> includeModules = emptyForNull(getIncludeModules());
		boolean filterForChanges = changes || changesUncommitted || changesUnpublished;

		boolean noFilter = isNullOrEmpty(includeModules) && isNullOrEmpty(excludeModules) && isNullOrEmpty(branches) && !filterForChanges;
		if (noFilter) {
			handleNoFilterSpecified();
		}

		Filter includeFilter = Filters.or(transform(includeModules, new Function<String, Filter>() {
			@Override
			public Filter apply(String input) {
				return new NameMatcherFilter(Pattern.compile(input));
			}
		}));
		Filter excludeFilter = Filters.and(transform(emptyForNull(excludeModules), new Function<String, Filter>() {
			@Override
			public Filter apply(String input) {
				return Filters.not(new NameMatcherFilter(Pattern.compile(input)));
			}
		}));
		Filter branchFilter = Filters.or(transform(emptyForNull(branches), new Function<String, Filter>() {
			@Override
			public Filter apply(String input) {
				return new BranchFilter(Pattern.compile(input));
			}
		}));
		Filter changeFilter;
		if (filterForChanges) {
			changeFilter = new ChangedFilter(changes || changesUncommitted, changes || changesUnpublished);
		} else {
			changeFilter = Filters.all();
		}
		Filter filter = Filters.and(includeFilter, excludeFilter, branchFilter, changeFilter);

		logger.debug("Filtering modules by: {}", filter);
		Collection<Module> modules = pride.getModules(filter);

		if (modules.isEmpty()) {
			handleNoMatchingModulesFound();
		} else {
			executeInModules(pride, modules);
		}
	}

	private static boolean isNullOrEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	private static <T> Collection<T> emptyForNull(Collection<T> collection) {
		return collection == null ? Collections.<T> emptySet() : collection;
	}

	protected abstract void executeInModules(Pride pride, Collection<Module> modules) throws Exception;

	protected abstract Collection<String> getIncludeModules();

	protected void handleNoFilterSpecified() {
		// Do nothing by default
	}

	protected void handleNoMatchingModulesFound() {
		// Do nothing by default
	}
}
