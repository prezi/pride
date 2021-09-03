package com.prezi.pride.test

class HgPrideDoStatusTest extends AbstractIntegrationSpec implements HgTestSupport {
	def "pride do status on a pristine repo"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		expect:
		pride([workingDir: 'pride'], ['do', '--', 'hg', 'status']) { process ->
			def lines = process.in.readLines()
			assert lines.size() == 1
			assert lines.first().startsWith('[1/1]')
			process.waitForProcessOutput()
			assert process.exitValue() == 0
		}
	}

	def "pride do status on a repo with uncommited changes"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		and:
		file('pride/project/new-file').text = ''

		expect:
		pride([workingDir: 'pride'], ['do', '--', 'hg', 'status']) { process ->
			assert process.in.text.contains('? new-file')
			process.waitForProcessOutput()
			assert process.exitValue() == 0
		}
	}
}
