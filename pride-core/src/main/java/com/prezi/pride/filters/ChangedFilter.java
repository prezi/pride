package com.prezi.pride.filters;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.prezi.pride.Module;
import com.prezi.pride.Pride;
import com.prezi.pride.vcs.VcsStatus;

import java.io.IOException;
import java.util.List;

public class ChangedFilter implements Filter {

	private final boolean uncommittedChanges;
	private final boolean unpublishedChanges;

	public ChangedFilter(boolean uncommittedChanges, boolean unpublishedChanges) {
		this.uncommittedChanges = uncommittedChanges;
		this.unpublishedChanges = unpublishedChanges;
	}

	@Override
	public boolean matches(Pride pride, Module module) throws IOException {
		VcsStatus status = module.getVcs().getSupport().getStatus(pride.getModuleDirectory(module.getName()));
		return (uncommittedChanges && status.hasUncommittedChanges()) || (unpublishedChanges && status.hasUnpublishedChanges());
	}

	@Override
	public String toString() {
		List<String> changeTypes = Lists.newArrayList();
		if (uncommittedChanges) {
			changeTypes.add("uncommitted");
		}
		if (unpublishedChanges) {
			changeTypes.add("unpublished");
		}
		return "HAS " + Joiner.on(" OR ").join(changeTypes) + " CHANGES";
	}
}
