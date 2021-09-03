package com.prezi.pride.test

class HgPrideListTest extends AbstractIntegrationSpec implements HgTestSupport {
	def "pride list on a pristine repo"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		expect:
		pride([workingDir: 'pride'], ['list']) { process ->
			assert process.in.text.startsWith('   project')
			process.waitForProcessOutput()
			assert process.exitValue() == 0
		}
	}

	def "pride list on a repo with uncommited changes"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		and:
		file('pride/project/new-file').text = ''

		expect:
		pride([workingDir: 'pride'], ['list']) { process ->
			assert process.in.text.startsWith(' M project')
			process.waitForProcessOutput()
			assert process.exitValue() == 0
		}
	}

	def "pride list on a repo with unpublished commits"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		and:
		commitNewEmptyFile('pride/project', 'new-file')

		expect:
		pride([workingDir: 'pride'], ['list']) { process ->
			assert process.in.text.startsWith('M  project')
			process.waitForProcessOutput()
			assert process.exitValue() == 0
		}
	}
}
