Pride
=====

Pride is an internal Prezi tool to manage a pride of Gradle-built modules stored in different GitHub repositories.

### How does it work?

Pride's central concept is a session. It's a (preferably) relatively short-lived entity that lets you work on a few modules of a large system. The idea is that you pick a few modules, start a session, do your work, commit, push, and then remove the session completely.

A session is a directory containing clones of modules. Each module is expected to have a Gradle build (i.e. a `build.gradle` file in its root directory). Pride generates Gradle build files in the session directory which allows you to build your modules together, without having to install them to an external repository (such as Ivy local in `~/.ivy2`).

## Get Pride

TBD

## Usage

### Command line

Pride has an extensive help system (much similar to Git), so it's easy to start with:

    $ pride help

To create an empty session, simply create an empty directory, and say:

    $ pride session init

To add modules by cloning them from GitHub you can use:

    $ pride session add <repo-name>

Where `<repo-name>` is the name of the Git repository under `https://github.com/prezi/`.

## Software used

* Gradle (http://gradle.org) to build stuff
* Airline (https://github.com/airlift/airline) for command-line handling
