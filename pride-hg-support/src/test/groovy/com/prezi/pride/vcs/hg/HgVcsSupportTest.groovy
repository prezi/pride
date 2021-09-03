package com.prezi.pride.vcs.hg

import org.apache.commons.configuration.MapConfiguration
import spock.lang.Specification
import spock.lang.Unroll

class HgVcsSupportTest extends Specification {
	def config = new MapConfiguration([:])
	def support = new HgVcsSupport(config)

	@Unroll
	def "module name resolution: #input vs #name"() {
		def result = support.resolveRepositoryName(input)

		expect:
		result == name

		where:
		input                                         | name
		"lajos"                                       | null
		"https://mbezjak@bitbucket.org/pypy/pypy"     | "pypy"
	}
}
