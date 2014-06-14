package com.prezi.gradle.pride.cli.commands;

import io.airlift.command.Command;

@Command(name = "rm", hidden = true, description = "Remove modules from a pride")
public class RmCommandAlias extends RemoveCommand {
}
