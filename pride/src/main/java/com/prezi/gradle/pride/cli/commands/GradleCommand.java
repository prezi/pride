package com.prezi.gradle.pride.cli.commands;

import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import com.prezi.gradle.pride.cli.gradle.GradleProjectExecution;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import org.gradle.tooling.ProjectConnection;

import java.io.File;
import java.util.List;

@Command(name = "gradle", description = "Run Gradle from the root of the pride")
public class GradleCommand extends AbstractPrideCommand {
	@Arguments
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private List<String> arguments;

	@Override
	public void executeInPride(Pride pride) throws Exception {
		new GradleConnectorManager(pride.getConfiguration()).executeInProject(pride.getRootDirectory(), new GradleProjectExecution<Void, RuntimeException>() {
			@Override
			public Void execute(File directory, ProjectConnection connection) {
				connection.newBuild()
						.withArguments(arguments.toArray(new String[arguments.size()]))
						.run();
				return null;
			}
		});
	}
}
