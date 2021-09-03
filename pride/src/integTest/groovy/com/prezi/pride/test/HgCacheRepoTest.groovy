package com.prezi.pride.test

class HgCacheRepoTest extends AbstractIntegrationSpec implements HgTestSupport {
	def "repo should be reinitialized if found stale"() {
		given:
		initializeHgRepo('project')
		ensurePrideModuleIsCached('project')
		makeModuleRepoUnrelatedToTheOneInPrideCache('project')

		when:
		initializePrideWithModule(cache: true, 'pride', 'project')

		then:
		notThrown(Exception)

		when:
		commitNewEmptyFile('project', 'new-file')
		pride workingDir: 'pride', 'update'

		then:
		file('pride/project/new-file').isFile()
	}

	private void ensurePrideModuleIsCached(String moduleName) {
		def tempWorkingDir = file('temporary-pride-working-directory-used-to-init-cache')
		try {
			initializePrideWithModule(cache: true, tempWorkingDir, moduleName)
			assert tempWorkingDir.isDirectory()
		} finally {
			assert tempWorkingDir.deleteDir()
		}
	}

	private void makeModuleRepoUnrelatedToTheOneInPrideCache(Object repoDir) {
		assert new File(file(repoDir), '.hg').deleteDir()
		initializeHgRepo(repoDir)
	}
}
