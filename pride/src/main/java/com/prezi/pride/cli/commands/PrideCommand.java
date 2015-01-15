package com.prezi.pride.cli.commands;

import java.util.concurrent.Callable;

public interface PrideCommand extends Callable<Integer> {
	boolean isVerbose();

	boolean isQuiet();

	boolean isHelp();
}
