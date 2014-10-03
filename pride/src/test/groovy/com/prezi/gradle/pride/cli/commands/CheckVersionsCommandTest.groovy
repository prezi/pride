package com.prezi.gradle.pride.cli.commands

import spock.lang.Specification
import spock.lang.Unroll

class CheckVersionsCommandTest extends Specification {
	@Unroll
	def "MatchVersion #requested == #actual: #expected"() {
		expect:
		CheckVersionsCommand.matchVersion(requested, actual) == expected

		where:
		requested | actual  | expected
		"1.0"     | "1.0"   | true
		"1.0"     | "100"   | false
		"1.0"     | "2.0"   | false
		"1.0"     | "1.0.1" | false
		"1.+"     | "1.0"   | true
		"1.+"     | "1.0.1" | true
		"1.+"     | "2.0"   | false
	}
}
