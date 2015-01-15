package com.prezi.pride.cli.commands;

import com.google.common.collect.ImmutableList;
import com.prezi.pride.cli.PridePlugin;

import java.util.Collection;

public class DefaultPridePlugin implements PridePlugin {
	@Override
	@SuppressWarnings("unchecked")
	public Collection<Class<? extends PrideCommand>> getCommands() {
		return ImmutableList.<Class<? extends PrideCommand>>of(
				AddCommand.class,
				CheckVersionsCommand.class,
				ConfigCommand.class,
				DoCommand.class,
				ExportCommand.class,
				GradleCommand.class,
				InitCommand.class,
				ListCommand.class,
				ReinitCommand.class,
				RemoveCommand.class,
				RmCommandAlias.class,
				UpdateCommand.class,
				VersionCommand.class
		);
	}
}
