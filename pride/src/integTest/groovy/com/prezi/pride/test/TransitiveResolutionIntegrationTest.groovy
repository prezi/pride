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
		gradle gradlewDir: rootDir, workingDir: moduleADir, "uploadArchives"
		gradle gradlewDir: rootDir, workingDir: moduleBDir, "uploadArchives"
		pride workingDir: prideDir, "init", "-v", "--gradle-version", defaultGradleVersion

		expect:
		gradle workingDir: prideDir, ["module-c:dependencies", "--configuration", "compileClasspath"], { Process process ->
			assert process.text.contains("""\\--- com.prezi.example.transitive:module-a:1.0 -> project :module-a""")
			process.waitForProcessOutput()
			assert process.exitValue() == 0
		}
	}
}
