package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.cli.PrideConfiguration
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "init", description = "Initialize pride")
class InitCommand extends AbstractPrideCommand {

	@Option(name = ["-f", "--force"],
			description = "Force initialization of a pride, even if one already exists")
	private boolean overwrite

	@Option(name = ["-T", "--repo-type"],
			title = "type",
			description = "Repository type (used to identify the type of any existing repos)")
	private String explicitRepoType

	@Option(name = "--no-add-existing",
			description = "Do not automatically add")
	boolean explicitNoAddExisting

	@Override
	public void run() {
		if (!overwrite && Pride.containsPride(prideDirectory)) {
			throw new PrideException("A pride already exists in ${prideDirectory}")
		}
		def pride = Pride.create(prideDirectory, vcsManager)
		def vcs = vcsManager.getVcs(explicitRepoType ?: configuration.getString(PrideConfiguration.REPO_TYPE_DEFAULT))

		if (!explicitNoAddExisting) {
			log.debug "Adding existing modules"
			def addedAny = false
			prideDirectory.eachDir { File dir ->
				if (Pride.isValidModuleDirectory(dir)) {
					log.info "Addign existing ${vcs.type} module in ${dir}"
					pride.addModule(dir.name, vcs)
					addedAny = true
				}
			}
			if (addedAny) {
				pride.save()
				pride.reinitialize()
			}
		}
	}
}
