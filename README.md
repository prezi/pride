Pride
=====

Pride is a tool to manage a pride of Gradle-built modules stored in different GitHub repositories.

[![Build Status](https://travis-ci.org/prezi/pride.svg?branch=master)](https://travis-ci.org/prezi/pride)

### How does it work?

Pride's central concept is a working session that spans a set of modules. In this case a "module" is a Git repository with a Gradle build.

The idea is that you pick a few modules, start a session (a "pride"), do your work, commit, push, and then remove the pride completely. A pride should be short-lived, but you can have several concurrent prides side-by side. You can also have the same module as part of different prides.

A pride is a directory containing clones of the modules that make up the pride. Each module is expected to have a Gradle build (i.e. a `build.gradle` file in its root directory). Pride generates Gradle build files in the pride directory which allows you to build your modules together, without having to install them to an external repository (such as Ivy local in `~/.ivy2`).

It is important to craft your modules so that they remain buildable on their own (*stand-alone mode*) as well as a part of a pride (*pride-mode*). In most cases you don't have to do anything special to make this happen, but there are some [limitations and caveats](#limitations-and-caveats) to watch out for.

## Get Pride

### Prerequisites

Some tools need to be available on the path:

* [Gradle](http://gradle.org/)
* [Git](http://git-scm.org/)

### Installing Pride

If you want to install the newest version of Pride, or upgrade your existing installation, run this:

    $ curl -sSLO http://git.io/install-pride && gradle -q -b install-pride && rm install-pride > /dev/null

This will install a symlink in `/usr/local/bin`.

If you are installing Pride for the first time, it's recommended to set the base URL for all your Git repositories, for example:

    $ pride config repo.base.url git@github.com:prezi

So when you execute `pride add some-module`, it will clone `git@github.com/prezi/some-module`.

### Building from source

If you want to experiment with Pride:

```shell
git clone git@github.com:prezi/pride.git
cd pride
gradle installApp
export PATH=$PATH:`pwd`/pride/build/install/pride/bin
```

Note: On Windows you will need to add `pride/build/install/pride/bin` to the `PATH` manually.

Check if everything works via:

    $ pride version
    Pride version 0.2

## Usage

### Command line

Pride has an extensive help system (much similar to Git), so it's easy to start with:

    $ pride help

To create a new pride do this in an empty directory:

    $ pride init

To add modules by cloning them from GitHub use:

    $ pride add <repo-name>

Where `<repo-name>` is the name of the Git repository under `https://github.com/prezi/`.

### The `pride` plugin

Pride has some additional functionalities to dependency resolution, so you need to apply the `pride` plugin on all your projects where you want to use these. This should be simple:

```groovy
buildscript {
    dependencies {
        classpath "com.prezi.gradle.pride:gradle-pride-plugin:0.2"
    }
}

apply plugin: "pride"
```

TBD: Where to get the plugin from, and which version to use?

## Repo caching

To quickly create (and discard) prides, Git repos of modules are cached locally. Here's what Pride does when you `add` a module with cache enabled:

* checks in its cache directory if it already has a clone of the module
    * if it doesn't exist, it creates a mirror clone of it (see `--mirror` in [git-clone](http://git-scm.com/docs/git-clone))
    * if it exists, it does a `git fetch --all` on it
* clones the cached repo to your pride
* sets `origin` to point to the original repo

You can disable caching on a per-repo basis by using `pride add --no-repo-cache repo-name`. Or you can disable it by setting this in `~/.prideconfig`:

    repo.cache.always=false

## Limitations and caveats

* Module dependencies can only be resolved properly to local projects available in the pride if they are specified via the `moduleDependencies { ... }` block (provided by the `pride` plugin) instead of `dependencies { ... }`. If you put them in `dependencies { ... }`, they will always come from Artifactory.
* Multi-project Gradle builds cannot use `project(":some-other-subproject")` and `project(path: "...")` to refer to other subprojects in the project, as Gradle does not support relative paths that point above the current project. Instead of these you can use `relativeProject(":some-other-subproject")` and `relativeProject(path: "...")`. These methods are also provided by the `pride` plugin. These will be resolved properly both in stand-alone and pride-mode.
* Do not use `gradle.properties` to store version numbers. It should not be needed, as in `moduleDependencies { ... }` you can specify the major version to depend on, and Gradle will always get you either a local project from the pride, or the newest version from Artifactory.
* Only use `include(...)` in `settings.gradle` -- Pride needs to merge all module's `settings.gradle`s, and it does not support arbitrary code.
* Do not use `buildSrc` to store your additional build logic. It's not a very good feature to start with, and Pride doesn't support it. Apply additional build logic from `something.gradle` instead.

## Why the name?

Working with a large modular application is like herding cats. Dangerous cats. Like a pride of lions.

![](http://i62.tinypic.com/2hs3g4o.jpg)

## Software used

* Gradle (http://gradle.org) to build stuff
* Airline (https://github.com/airlift/airline) for command-line handling
