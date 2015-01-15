package com.prezi.pride.vcs.git

import org.apache.commons.configuration.MapConfiguration
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by lptr on 09/06/14.
 */
class GitVcsSupportTest extends Specification {
	def config = new MapConfiguration([:])
	def support = new GitVcsSupport(config)

	@Unroll
	def "module name resolution: #input vs #name"() {
		def result = support.resolveRepositoryName(input)

		expect:
		result == name

		where:
		input                                         | name
		"lajos"                                       | null
		"git@github.com:prezi/lajos.git/"             | "lajos"
		"tibor_123@github.com:prezi/lajos.git/"       | "lajos"
		"ssh://git@github.com:prezi/lajos.git/"       | "lajos"
		"http://bela:geza@github.com/prezi/lajos.git" | "lajos"
		"https://github.com/prezi/lajos"              | "lajos"
		"ftps://github.com/prezi/lajos.git"           | "lajos"
		"rsync://github.com/prezi/lajos/"             | "lajos"
	}
}
