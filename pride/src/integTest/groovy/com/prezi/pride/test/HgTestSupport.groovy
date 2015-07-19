package com.prezi.pride.test

trait HgTestSupport {

  void hgInit(Object repoDir) {
    exec workingDir: repoDir, 'hg', 'init'
  }

  void writeStubHgrc(Object repoDir) {
    new File(file(repoDir), '.hg/hgrc').text = """
        [ui]
        username = Spock <spock@example.com>

        [paths]
        default = https://bitbucket.org/prezi/test
    """.stripIndent()
  }

  void commitAll(Object repoDir, String message) {
    exec workingDir: repoDir, 'hg', 'add', '.'
    exec workingDir: repoDir, 'hg', 'commit', '--message', message
  }

  void initializeHgRepo(Object repoDir) {
    hgInit(repoDir)
    writeStubHgrc(repoDir)
    commitAll(repoDir, 'Initial')
  }

  void commitNewEmptyFile(Object repoDir, String fileName) {
    new File(file(repoDir), fileName).text = ''
    commitAll(repoDir, "Add new empty file: $fileName")
  }

  void commitNewFileWithText(Object repoDir, String fileName, String text) {
    new File(file(repoDir), fileName).text = text
    commitAll(repoDir, "Add new file '$fileName' with text '$text'")
  }

  void prideAdd(Map opts, Object workingDir, String moduleName) {
    def baseUrl = "file://${dir.absolutePath}"
    def cacheArg = opts.cache ? '--use-repo-cache' : '--no-repo-cache'

    pride workingDir: workingDir,
          'add', '--verbose', cacheArg,
          '--repo-base-url', baseUrl, '--repo-type', 'hg',
          moduleName
  }

  void initializePrideWithModule(Map opts = [:], Object workingDir, String moduleName) {
    assert file(workingDir).mkdir()
    pride workingDir: workingDir, 'init'
    prideAdd opts, workingDir, moduleName
  }

}
