package com.prezi.pride.test

import org.apache.commons.io.FileUtils

class TransitiveResolutionIntegrationTest extends AbstractIntegrationSpec {
	def "transitive dependency resolution"() {
		given:
		def repoDir = new File(buildDir, "repo")
		def prideDir = file("pride")
		def moduleADir = file("pride/module-a")
		def moduleBDir = file("module-b")

		FileUtils.forceMkdir repoDir
		gradle workingDir: moduleADir, "uploadArchives"
		gradle workingDir: moduleBDir, "uploadArchives"
		pride workingDir: prideDir, "init", "-v", "--gradle-version", defaultGradleVersion

		expect:
		gradle workingDir: prideDir, ["module-c:dependencies", "--configuration", "compile"], { Process process ->
			assert process.text.contains("""
compile - Compile classpath for source set 'main'.
\\--- com.prezi.example.transitive:module-b:1.0
     \\--- com.prezi.example.transitive:module-a:1.0 -> project :module-a
""")
			process.waitForProcessOutput()
			assert process.exitValue() == 0
		}

		gradle workingDir: prideDir, ["module-c:checkDependencyVersions", "--configuration", "compile"], { process ->
			assert process.text.contains("""
Configuration "compile" in project ":module-c" requests version 1.0 of project ":module-a", but its current version (1.1) does not fulfill that request
""")
			process.waitForProcessOutput()
			assert process.exitValue() == 0
		}
	}
}
