package com.prezi.gradle.pride.cli.commands;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.ExportedModule;
import com.prezi.gradle.pride.cli.commands.actions.AddAction;
import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsSupport;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.List;

import static com.prezi.gradle.pride.cli.Configurations.REPO_BASE_URL;
import static com.prezi.gradle.pride.cli.Configurations.REPO_BRANCH;
import static com.prezi.gradle.pride.cli.Configurations.REPO_TYPE_DEFAULT;

@Command(name = "add", description = "Add modules to a pride")
public class AddCommand extends AbstractPrideCommand {

	@Option(name = {"-O", "--overwrite"},
			description = "Overwrite existing modules in the pride")
	private boolean overwrite;

	@Option(name = {"-B", "--repo-base-url"},
			title = "url",
			description = "Base URL for module repositories")
	private String explicitRepoBaseUrl;

	@Option(name = {"-c", "--use-repo-cache"},
			description = "Use local repo cache")
	private boolean explicitUseRepoCache;

	@Option(name = {"--no-repo-cache"},
			description = "Do not use local repo cache")
	private boolean explicitNoRepoCache;

	@Option(name = {"-r", "--recursive"},
			description = "Update sub-modules recursively")
	private Boolean explicitRecursive;

	@Option(name = {"-T", "--repo-type"},
			title = "type",
			description = "Repository type")
	private String explicitRepoType;

	@Option(name = {"-b", "--branch"},
			title = "branch",
			description = "Branch to use")
	private String explicitBranch;

	@Arguments(description = "Modules to add to the pride -- either module names to be resolved against the base URL, or full repository URLs")
	private List<String> modules;

	@Override
	public void executeInPride(Pride pride) throws Exception {
		if (modules == null || modules.isEmpty()) {
			throw new PrideException("No modules specified");
		}
		AddAction addAction = new AddAction(pride, overwrite, explicitUseRepoCache, explicitNoRepoCache, explicitRecursive, isVerbose());
		addAction.addModules(getModulesToAdd(pride.getConfiguration()));
	}

	private Collection<ExportedModule> getModulesToAdd(RuntimeConfiguration config) {
		String repoType = config.override(REPO_TYPE_DEFAULT, explicitRepoType);
		final String repoBaseUrl = config.override(REPO_BASE_URL, explicitRepoBaseUrl);
		final String branch = config.override(REPO_BRANCH, explicitBranch);

		final Vcs vcs = getVcsManager().getVcs(repoType, config);
		final VcsSupport vcsSupport = vcs.getSupport();

		return Collections2.transform(modules, new Function<String, ExportedModule>() {
			@Override
			public ExportedModule apply(String module) {
				String moduleName = vcsSupport.resolveRepositoryName(module);
				String repoUrl;
				if (!StringUtils.isEmpty(moduleName)) {
					repoUrl = module;
				} else {
					moduleName = module;
					repoUrl = getRepoUrl(repoBaseUrl, moduleName);
				}
				return new ExportedModule(moduleName, repoUrl, branch, vcs);
			}
		});
	}

	private static String getRepoUrl(String repoBaseUrl, String moduleName) {
		if (repoBaseUrl == null) {
			throw new PrideException("You have specified a module name, but base URL for repositories is not set. " +
					"Either use a full repository URL, specify the base URL via --repo-base-url, " +
					"or set it in the global configuration (~/.prideconfig) as \"" + REPO_BASE_URL + "\". " +
					"See \'pride help config\' for more information.");
		}

		if (!repoBaseUrl.endsWith("/")) {
			repoBaseUrl += "/";
		}

		return repoBaseUrl + moduleName;
	}
}
