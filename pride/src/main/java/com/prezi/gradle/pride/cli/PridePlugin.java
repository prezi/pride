package com.prezi.gradle.pride.cli;

import com.prezi.gradle.pride.cli.commands.PrideCommand;

import java.util.Collection;

public interface PridePlugin {
	Collection<Class<? extends PrideCommand>> getCommands();
}
