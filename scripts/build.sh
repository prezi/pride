#!/bin/bash -ex

export JAVA_HOME=/usr/lib/jvm/java-7-oracle

if [ -e /etc/profile.d/gradle.sh ]; then
    . /etc/profile.d/gradle.sh
fi

gradle clean uploadArchives --info --stacktrace -Prelease
