package com.prezi.pride.test

class FailingModuleBuildIntegrationTest extends AbstractIntegrationSpec {
	def "failing module build"() {
		expect:
		pride(["init"]) { Process process ->
			assert process.err.text.contains("""You can get more detailed information about the error by rerunning Pride with --verbose.""")
			process.waitForProcessOutput()
			assert process.exitValue() != 0
		}
		pride(["init", "--force", "--verbose"]) { Process process ->
			def error = process.err.text
			assert !error.contains("""You can get more detailed information about the error by rerunning Pride with --verbose.""")
			assert error.contains("""Caused by: java.lang.RuntimeException: Error during configuration time""")
			process.waitForProcessOutput()
			assert process.exitValue() != 0
		}
	}
}
