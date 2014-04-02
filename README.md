Pride
=====

Pride is an internal Prezi tool to manage a pride of Gradle-built modules stored in different GitHub repositories.

### How does it work?

Pride's central concept is a session. The idea is that you pick a few modules, start a session, do your work, commit, push, and then remove the session completely. A session should be short-lived, but you can have several concurrent sessions side-by side.

A session is a directory containing clones of modules. Each module is expected to have a Gradle build (i.e. a `build.gradle` file in its root directory). Pride generates Gradle build files in the session directory which allows you to build your modules together, without having to install them to an external repository (such as Ivy local in `~/.ivy2`).

## Get Pride

### Prerequisites

* [Gradle](http://gradle.org/) and [Git](http://git-scm.org/) should be available on the PATH.
* It is also nice if you have your Prezi GitHub credentials set up before you proceed further.

### Installing and updating Pride

The easiest way to install Pride is by running:

    $ curl -sSLO Http://href.prezi.com/install-pride && gradle -q -b install-pride && rm install-pride > /dev/null

Currently this is also the way to upgrade it. An automatic upgrade facility is coming soon.

You can also download the Gradle build script from http://href.prezi.com/install-pride, and execute it manually by running `gradle -b install-pride`.

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

To create a new session do this in an empty directory:

    $ pride init

To add modules by cloning them from GitHub use:

    $ pride add <repo-name>

Where `<repo-name>` is the name of the Git repository under `https://github.com/prezi/`.

## Limitations and caveats

* Module dependencies can only be resolved properly to local projects available in the session if they are specified via the `moduleDependencies { ... }` block instead of `dependencies { ... }`. If you put them in `dependencies { ... }`, they will always come from Artifactory.
* Multi-project Gradle builds cannot use `project(":some-other-subproject")` to refer to other subprojects in the project. You should not this either, and it is a code-smell to use this feature. Publish anything you might need in other subprojects to a configuration, and depend on that instead.
* Do not use `gradle.properties` to store version numbers. It should not be needed, as in `moduleDependencies { ... }` you can specify the major version to depend on, and Gradle will always get you either a local project from the session, or the newest version from Artifactory.
* Only use `include(...)` in `settings.gradle` -- Pride needs to merge all module's `settings.gradle`s, and it does not support arbitrary code.
* Pride merges `gradle.properties` from modules into a `gradle.properties` in the root of the session. If multiple modules define the same property, the results might be confusing.
* Do not use `buildSrc` to store your additional build logic. It's not a very good feature to start with, and Pride doesn't support it. Apply additional build logic from `something.gradle` instead.

## Why the name?

Working with a large modular application is like herding cats. Dangerous cats. Like a pride of lions.

![](http://i62.tinypic.com/2hs3g4o.jpg)

## Software used

* Gradle (http://gradle.org) to build stuff
* Airline (https://github.com/airlift/airline) for command-line handling
