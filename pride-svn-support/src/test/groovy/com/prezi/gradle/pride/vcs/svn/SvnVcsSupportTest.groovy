package com.prezi.gradle.pride.vcs.svn

import spock.lang.Specification
import spock.lang.Unroll

class SvnVcsSupportTest extends Specification {
	def support = new SvnVcsSupport()

	@Unroll
	def "module name resolution: #input vs #name"() {
		def result = support.resolveRepositoryName(input)

		expect:
		result == name

		where:
		input                                         | name
		"lajos"                                       | null
		"http://bela:geza@github.com/prezi/lajos.git" | "lajos"
		"https://github.com/prezi/lajos"              | "lajos"
		"svn://github.com/prezi/lajos.git"            | "lajos"
	}
}
