Pride
=====

Pride is an internal Prezi tool to manage a pride of Gradle-built modules stored in different GitHub repositories.

### How does it work?

Pride's central concept is a session. The idea is that you pick a few modules, start a session, do your work, commit, push, and then remove the session completely. A session should be short-lived, but you can have several concurrent sessions side-by side.

A session is a directory containing clones of modules. Each module is expected to have a Gradle build (i.e. a `build.gradle` file in its root directory). Pride generates Gradle build files in the session directory which allows you to build your modules together, without having to install them to an external repository (such as Ivy local in `~/.ivy2`).

## Get Pride

### Installing and updating Pride

Execute this to install the newest version of Pride:

    $ curl -sSL http://href.prezi.com/install-pride | gradle

Currently this is also the way to upgrade it. An automatic upgrade facility is coming soon.

### Building from source

If you want to experiment with Pride:

```shell
git clone git@github.com:prezi/pride.git
cd pride
gradle installApp
export PATH=$PATH:`pwd`/build/install/pride/bin
```

Note: On Windows you will need to add `build/install/pride/bin` to the `PATH` manually.

Check if everything works via:

    $ pride version
    Pride version 0.1-27-ge342f73

## Usage

### Command line

Pride has an extensive help system (much similar to Git), so it's easy to start with:

    $ pride help

To create an empty session, simply create an empty directory, and say:

    $ pride init

To add modules by cloning them from GitHub you can use:

    $ pride add <repo-name>

Where `<repo-name>` is the name of the Git repository under `https://github.com/prezi/`.

## Software used

* Gradle (http://gradle.org) to build stuff
* Airline (https://github.com/airlift/airline) for command-line handling
