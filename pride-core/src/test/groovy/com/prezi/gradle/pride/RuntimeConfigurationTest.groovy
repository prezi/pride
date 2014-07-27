package com.prezi.gradle.pride

import org.apache.commons.configuration.BaseConfiguration
import spock.lang.Specification
import spock.lang.Unroll

import static com.prezi.gradle.pride.RuntimeConfiguration.*

@SuppressWarnings("GroovyPointlessBoolean")
class RuntimeConfigurationTest extends Specification {
	@Unroll
	def "override in #name expecting #expected"() {
		expect:
		config.containsKey(GRADLE_HOME) == expected != null
		config.getString(GRADLE_HOME) == expected

		when:
		config.override(GRADLE_HOME, "test")

		then:
		config.containsKey(GRADLE_HOME) == true
		config.getString(GRADLE_HOME) == "test"

		where:
		name           | config         | expected
		"no config"    | create()       | null
		"global only"  | globalConfig() | "testGlobal"
		"pride config" | prideConfig()  | "testPride"
	}

	RuntimeConfiguration globalConfig() {
		def global = new BaseConfiguration()
		global.setProperty(GRADLE_HOME, "testGlobal")
		return create(global)
	}

	RuntimeConfiguration prideConfig() {
		def pride = new BaseConfiguration()
		pride.setProperty(GRADLE_HOME, "testPride")
		return globalConfig().withConfiguration(pride)
	}
}
