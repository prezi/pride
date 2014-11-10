package com.prezi.gradle.pride.cli.commands;

import java.util.concurrent.Callable;

public interface PrideCommand extends Callable<Integer> {
	boolean isVerbose();

	boolean isQuiet();

	boolean isHelp();
}
