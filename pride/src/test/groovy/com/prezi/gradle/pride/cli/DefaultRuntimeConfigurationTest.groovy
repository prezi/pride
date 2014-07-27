package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.RuntimeConfiguration
import org.apache.commons.configuration.BaseConfiguration
import spock.lang.Specification
import spock.lang.Unroll

import static com.prezi.gradle.pride.cli.Configurations.GRADLE_HOME

@SuppressWarnings("GroovyPointlessBoolean")
class DefaultRuntimeConfigurationTest extends Specification {
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
		"no config"    | none()       | null
		"global only"  | globalConfig() | "testGlobal"
		"pride config" | prideConfig()  | "testPride"
	}

	RuntimeConfiguration none() {
		return DefaultRuntimeConfiguration.create()
	}

	RuntimeConfiguration globalConfig() {
		def global = new BaseConfiguration()
		global.setProperty(GRADLE_HOME, "testGlobal")
		return DefaultRuntimeConfiguration.create(global)
	}

	RuntimeConfiguration prideConfig() {
		def pride = new BaseConfiguration()
		pride.setProperty(GRADLE_HOME, "testPride")
		return globalConfig().withConfiguration(pride)
	}
}
