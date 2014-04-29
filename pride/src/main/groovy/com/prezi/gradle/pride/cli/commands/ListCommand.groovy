package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Module
import com.prezi.gradle.pride.Pride
import io.airlift.command.Command
import io.airlift.command.Option

/**
 * Created by lptr on 30/04/14.
 */
@Command(name = "list", description = "Lists modules in a pride")
class ListCommand extends AbstractExistingPrideCommand {

	@Option(name = ["-m", "--modules"],
			description = "Show only the modules in the pride")
	private boolean explicitModules

	@Option(name = ["-s", "--short"],
			description = "Show only module names")
	private boolean explicitShort

	@Override
	void runInPride(Pride pride) {
		if (explicitShort || explicitModules) {
			pride.modules.each { Module module ->
				log.info formatModule(module, explicitShort)
			}
		} else {
			pride.rootDirectory.eachDir { File dir ->
				if (!Pride.isValidModuleDirectory(dir)) {
					return
				}
				if (pride.hasModule(dir.name)) {
					log.info formatModule(pride.getModule(dir.name), false)
				} else {
					log.info "? ${dir.name}"
				}
			}
		}
	}

	private static def formatModule(Module module, boolean onlyNames) {
		return onlyNames ? module.name : "m ${module.name} (${module.vcs.type})"
	}
}
