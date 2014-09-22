package com.prezi.gradle.pride.filters;

import com.google.common.base.Strings;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;

import java.io.IOException;
import java.util.regex.Pattern;

public class BranchFilter implements Filter {
	private final Pattern branchPattern;

	public BranchFilter(Pattern branchPattern) {
		this.branchPattern = branchPattern;
	}

	@Override
	public boolean matches(Pride pride, Module module) throws IOException {
		String branch = module.getBranch();
		if (Strings.isNullOrEmpty(branch)) {
			branch = module.getVcs().getSupport().getDefaultBranch();
		}
		return branchPattern.matcher(branch).matches();
	}

	@Override
	public String toString() {
		return "BRANCH = /" + branchPattern.pattern() + "/";
	}
}
