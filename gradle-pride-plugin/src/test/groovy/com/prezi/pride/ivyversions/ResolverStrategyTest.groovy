package com.prezi.pride.ivyversions

import spock.lang.Specification
import spock.lang.Unroll

class ResolverStrategyTest extends Specification {
	@Unroll
	def "Match version #requested == #actual: #expected"() {
		expect:
		new ResolverStrategy().accept(requested, actual) == expected

		where:
		requested   | actual  | expected
		"1.0"       | "1.0"   | true
		"1.0"       | "100"   | false
		"1.0"       | "2.0"   | false
		"1.0"       | "1.0.1" | false
		"1.+"       | "1.0"   | true
		"1.+"       | "1.0.1" | true
		"1.+"       | "2.0"   | false
		"{1}.+"     | "{1}.0" | true
		"[1.0,2.0)" | "1.0.1" | true
		"[1.0,2.0)" | "2.0.1" | false
	}
}
