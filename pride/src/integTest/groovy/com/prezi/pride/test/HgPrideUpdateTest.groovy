package com.prezi.pride.test

class HgPrideUpdateTest extends AbstractIntegrationSpec implements HgTestSupport {
	def "pride update on a pristine repo"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		and:
		commitNewEmptyFile('project', 'new-file')

		when:
		pride workingDir: 'pride', 'update'

		then:
		file('pride/project/new-file').isFile()
	}

	def "pride update on a repo that has unrelated changes"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		and:
		file('pride/project/unrelated-file').text = ""
		commitNewEmptyFile('project', 'file-from-origin-repo')

		when:
		pride workingDir: 'pride', 'update'

		then:
		file('pride/project/file-from-origin-repo').isFile()
		file('pride/project/unrelated-file').isFile()
	}

	def "pride update on a repo that has unpublished commits"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		and:
		commitNewEmptyFile('project', 'file-from-origin-repo')
		commitNewEmptyFile('pride/project', 'new-file-in-pride-module-repo')

		when:
		pride workingDir: 'pride', 'update'

		then:
		file('pride/project/file-from-origin-repo').isFile()
		file('pride/project/new-file-in-pride-module-repo').isFile()
	}

	def "pride update on a repo with conflicting changes WRT origin"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		and:
		commitNewFileWithText('pride/project', 'same-file', 'xyz')
		commitNewFileWithText('project', 'same-file', 'abc')

		expect:
		pride([workingDir: 'pride'], ['update']) { process ->
			assert process.in.text.contains("unresolved conflicts")
			process.waitForProcessOutput()
			assert process.exitValue() != 0
		}
	}

	def "pride update when origin no longer exists"() {
		given:
		initializeHgRepo('project')
		initializePrideWithModule('pride', 'project')

		and:
		assert file('project').deleteDir()

		expect:
		pride([workingDir: 'pride'], ['update']) { process ->
			assert process.in.text.contains("not found")
			process.waitForProcessOutput()
			assert process.exitValue() == 255
		}
	}
}
