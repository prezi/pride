Modularization vs. development speed: how Pride helps us work fast locally
==========================================================================

**[Pride](https://github.com/prezi/pride) is a tool we use internally at Prezi to help manage our local development of large modular applications built with Gradle. Here's why it might be useful to you as well.**

Building large applications is unavoidably trouble. What you get to choose is what kind of trouble you want to deal with. Some suggest [keeping everything in one, large, ever-growing piece](http://www.ustream.tv/recorded/46622121). We suffered the pains of a monolithic beast long enough to give a try to aggressive modularization with our upcoming JavaScript/HTML5 reimplementation of the Prezi editor. This post is about the tools we use to split our applications into modules without our developers losing the benefits of working on a single codebase.

## The trouble with modularization

When you split a large application into pieces, you have to face new problems like versioning,  dependency management. You have to ask yourself the question: "what do I do with the diamond problem?" There are pretty good solutions for these problems in the industry. In the JVM space Ivy, Maven and Gradle all successfully tackled the issue by building components separately, deploying the built artifacts to a repository, and downloading them when they are needed to build other components.

What they don't have an answer for is how to tie these modules together in the local development environment without resorting to the little dance ritual:

```bash
cd modulea
gradle install
# Go visit facebook for two minutes while the build finishes, and then forget about it, only to come back ten minutes later
cd ../moduleb
gradle install
# Go read email for fifteen minutes and forget if you installed modulea
cd ../modulea
gradle install
# Go check on Twitter and get lost in an Ars Technica article for half an hour
cd ../application
gradle runApplication
```

Wouldn't it be nice if your build system knew about how your modules fit together, what's available locally, and what needs to be downloaded from the artifact repository? So all you would need to do would be this:

```base
gradle runApplication
```

As you might have guessed, this is where Pride comes in.

## Pride: a walkthrough

Understanding how Pride works is easiest thorugh watching it work. To follow the tutorial below you will need to have Gradle (1.11 or newer) installed. Once you have that you are ready to jump in!

Installing Pride is simple:

```bash
$ curl -sSLO http://git.io/install-pride && gradle -q -b install-pride && rm install-pride > /dev/null
Pride version: 0.6
Created /usr/local/bin/pride
Successfully installed Pride version 0.6
```

This will install a symlink in `/usr/local/bin`. (Sorry, no installer for Windows yet.)

You are all set! Let's create an empty pride first:

```bash
$ mkdir pride-test
$ cd pride-test
$ pride init
Initializing /Users/lptr/pride-test
```

You are now ready to add some modules to the pride. We've prepared an example application for you:

```bash
$ pride add https://github.com/lptr/pride-example-application
Adding pride-example-application from https://github.com/lptr/pride-example-application
# ...
```

Now you have a pride with a single (Git) module in it. You can check with Pride:

```bash
$ pride list
m pride-example-application (git)
```

Okay, but what Gradle projects are in there? Because the pride is a Gradle project, you can simply ask Gradle:

```bash
$ gradle projects
:projects

------------------------------------------------------------
Root project
------------------------------------------------------------

Root project 'pride-test'
\--- Project ':pride-example-application'
```

You can check the dependencies of the application, too:

```bash
$ gradle pride-example-application:dependencies --configuration compile
:pride-example-application:dependencies

------------------------------------------------------------
Project :pride-example-application
------------------------------------------------------------

compile - Compile classpath for source set 'main'.
\--- com.example:pride-example-transformer:1.0-SNAPSHOT
     \--- com.example:pride-example-producer:1.0-SNAPSHOT
```

The application depends on the `1.0-SNAPSHOT` version of `pride-example-transformer`, which then further depends on `pride-example-producer`. They are both external dependencies, so they are downloaded from an external artifact repository (in our case `http://prezi.github.io/pride/example-repo/`).

Let's run the application:

```bash
$ gradle run
:pride-example-application:compileJava
:pride-example-application:processResources UP-TO-DATE
:pride-example-application:classes
:pride-example-application:run
Hello World!
```

That `Hello World!` there is the result of the code running.

Let's change the greeting to `Hello Gradle!`. The `World` string is supplied by the producer module. Let's make that project part of the pride:

```bash
$ pride add https://github.com/lptr/pride-example-producer
Adding pride-example-producer from https://github.com/lptr/pride-example-producer
# ...
```

It shouldn't be a surprise at this point that we now have two modules:

```bash
$ pride list
m pride-example-application (git)
m pride-example-producer (git)
```

How did the dependencies of the application change, though?

```bash
$ gradle pride-example-application:dependencies --configuration compile
:pride-example-application:dependencies

------------------------------------------------------------
Project :pride-example-application
------------------------------------------------------------

compile - Compile classpath for source set 'main'.
+--- com.example:pride-example-transformer:1.0-SNAPSHOT
|    \--- com.example:pride-example-producer:1.0-SNAPSHOT -> project :pride-example-producer
\--- project :pride-example-producer
```

After adding it to the pride, `pride-example-transformer` became a project dependency. Even though `pride-example-application` requested `1.0-SNAPSHOT`, it got the project instead (that's what the little `->` means).

(The separate first-level project dependency is added by Pride to tell Gradle to use the project instead of the external dependency. This workaround will be removed as soon as we can get the support for dynamic dependencies into Gradle itself.)

When you run the application again, Gradle will know about this project dependency, and will include it in the build. Notice that we have a lot more tasks in the build now:

```bash
$ gradle run
:pride-example-producer:compileJava
:pride-example-producer:processResources UP-TO-DATE
:pride-example-producer:classes
:pride-example-producer:jar
:pride-example-application:compileJava
:pride-example-application:processResources UP-TO-DATE
:pride-example-application:classes
:pride-example-application:run
Hello World!
```

Change the code of the producer and make the `produce()` method return `"Gradle"`:

```bash
$ vi pride-example-producer/src/main/java/com/example/producer/Producer.java
```

Now run your application again:

```bash
$ gradle run
:pride-example-producer:compileJava
:pride-example-producer:processResources UP-TO-DATE
:pride-example-producer:classes
:pride-example-producer:jar
:pride-example-application:compileJava
:pride-example-application:processResources UP-TO-DATE
:pride-example-application:classes
:pride-example-application:run
Hello Gradle!
```

## How does this work?

There are two tricks employed by Pride. The first one is to tell Gradle that these modules belong to a single über-project. Pride does this by generating a `settings.gradle` file in the root of your pride directory, listing all your modules. This file is regenerated whenever you add or remove projects.

```
.
|-- pride-example-application/
|   |-- src/
|   |-- LICENSE
|   |-- README.md
|   `-- build.gradle
|-- pride-example-producer/
|   |-- src/
|   |-- LICENSE
|   |-- README.md
|   `-- build.gradle|-- build.gradle     <------- this is
`-- settings.gradle  <------- generated
```

The second trick is to rewire Gradle's dependency resolution mechanism. There are two distinct ways to tell Gradle where a dependency comes from. You can refer to an external dependency loaded from an external artifact repository like Artifactory:

```groovy
dependencies {
	compile group: "com.google.inject", name: "guice", version: "3.0"
}
```

...or you can refer to another subproject in a multi-project build:

```groovy
dependencies {
	compile project(path: ":some-other-subproject")
}
```

What we want is to use the exetrnal dependency when a module is not available locally, and use the project dependency when it is. Unfortunately Gradle only allows you to choose one or the other, so we have to trick it. In a Pride-compatible module you define your dynamic dependencies much like how you would do normally, but in a block called `dynamicDependencies`:

```groovy
dynamicDependencies {
	compile group: "com.mycompany", name: "my-module", version: "4.3.7"
}
```

The Pride plugin converts this declaration into either an external or a project dependency depending on whether or not the referenced module is present in your pride. You will have to apply the plugin on your modules to make it work:

```groovy
buildscript {
	dependencies {
		classpath "com.prezi.gradle.pride:gradle-pride-plugin:0.6"
	}
}

apply plugin: "pride"
```

### IDE support

Because a pride is simply a big Gradle project, you can load your pride into any IDE that understands Gradle. In IntelliJ IDEA you can simply open the `build.gradle` in the root of your pride:

	Image of IntelliJ "Projects" view


## What else can you do with a pride?

A pride is a working session: you start it when you want to achieve something involving a number of modules. You add those modules to your pride. Once you are done, you can discard the whole thing. You can also have several prides set up simultaneously, even having the same module in many of them, perhaps working on different branches.

You've already seen `pride add` at work. To make creating and discarding, `pride add` caches your repositories, so when you use the same module in multiple prides, you only need to pull changes instead of having to re-clone it completely.

If you don't need a module in your pride anymore, it's easy to remove it with `pride remove`.

If you want to explore further, check out the other commands Pride offers:

```bash
$ pride help
usage: pride [(-q | --quiet)] [(-v | --verbose)] <command> [<args>]

The most commonly used pride commands are:
    add       Add modules to a pride
    config    Set configuration parameters
    do        Execute a command on a set of the modules
    help      Display help information
    init      Initialize pride
    list      Lists modules in a pride
    remove    Remove modules from a pride
    update    Updates a pride
    version   Display program version

See 'pride help <command>' for more information on a specific command.
```
