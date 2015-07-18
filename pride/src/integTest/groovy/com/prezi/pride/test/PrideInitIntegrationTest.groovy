package com.prezi.pride.test

class PrideInitIntegrationTest extends AbstractIntegrationSpec {
	def "pride init"() {
		given:
		setupModules()

		when:
		pride "init", "--gradle-version", defaultGradleVersion

		then:
		file("build.gradle").exists()
		file("gradlew").exists()
		file("gradlew.bat").exists()
		file("gradle").isDirectory()

		rawContents("settings.gradle", "//") == """
include 'file-module'
project(':file-module').projectDir = file('file-module')
include 'git-module'
project(':git-module').projectDir = file('git-module')
"""

		rawContents(".pride/version", "#") == """
0
"""

		asProps(file(".pride/config")) == asProps("""
gradle.version = ${defaultGradleVersion}
modules.0.name = file-module
modules.0.vcs = file
modules.1.name = git-module
modules.1.vcs = git
""")

		when:
		def exportFile = file("temp/export.pride")
		pride "export", "--output", exportFile, "-O"

		then:
		asProps(exportFile) == asProps("""
gradle.version = ${defaultGradleVersion}
modules.0.name = file-module
modules.0.vcs = file
modules.1.name = git-module
modules.1.vcs = git
modules.0.remote = ${dir}/file-module
modules.0.revision = none
modules.1.remote = git@github.com:prezi/test
modules.1.revision = master
""")

		when:
		pride "remove", "file-module"

		then:
		rawContents("settings.gradle", "//") == """
include 'git-module'
project(':git-module').projectDir = file('git-module')
"""
		asProps(file(".pride/config")) == asProps("""
gradle.version = ${defaultGradleVersion}
modules.0.name = git-module
modules.0.vcs = git
""")

	}

	def "using with earlier Gradle versions produces error"() {
		given:
		setupModules()
		pride "init", "--gradle-version", "2.4"

		expect:
		gradle(["tasks"]) { Process process ->
			assert process.err.text.contains("""Pride requires Gradle version 2.5 or later. If you want to use an earlier Gradle version, try Pride 0.10.""")
			process.waitForProcessOutput()
			assert process.exitValue() != 0
		}
	}

	def setupModules() {
		exec workingDir: "git-module", "git", "init"
		exec workingDir: "git-module", "git", "config", "user.email", "test@example.com"
		exec workingDir: "git-module", "git", "config", "user.name", "Test User"
		exec workingDir: "git-module", "git", "add", "-A"
		exec workingDir: "git-module", "git", "commit", "--message", "Initial"
		exec workingDir: "git-module", "git", "remote", "add", "origin", "git@github.com:prezi/test"
	}
}
