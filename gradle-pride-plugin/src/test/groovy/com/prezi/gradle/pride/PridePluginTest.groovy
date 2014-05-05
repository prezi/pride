package com.prezi.gradle.pride

import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import spock.lang.Specification

/**
 * Created by lptr on 28/04/14.
 */
class PridePluginTest extends Specification {
	def "unable to resolve to project dependency"() {
		given:
		def resolver = Mock(LocalProjectResolver)
		def dependency = Mock(ExternalDependency)
		def projects = [:]

		when:
		def actual = PridePlugin.localizeFirstLevelDynamicDependency(dependency, projects, resolver)

		then:
		_ * dependency.group >> "com.example"
		_ * dependency.name >> "test"
		_ * dependency.configuration >> "compile"
		actual == dependency
		0 * _
	}

	def "able to resolve to project dependency"() {
		given:
		def resolver = Mock(LocalProjectResolver)
		def dependency = Mock(ExternalDependency)
		def expected = Mock(ProjectDependency)
		def excludeRules = [ Mock(ExcludeRule) ]
		def mockExcludeRules = Mock(Set)
		def artifacts = [ Mock(DependencyArtifact) ]
		def mockArtifacts = Mock(Set)
		def projects = ["com.example:test": ":test"]

		when:
		def actual = PridePlugin.localizeFirstLevelDynamicDependency(dependency, projects, resolver)

		then:
		_ * dependency.group >> "com.example"
		_ * dependency.name >> "test"
		_ * dependency.configuration >> "compile"
		_ * dependency.transitive >> true
		actual == expected
		1 * dependency.excludeRules >> excludeRules
		1 * resolver.resolveLocalProject(":test", "compile") >> expected
		1 * expected.excludeRules >> mockExcludeRules
		1 * mockExcludeRules.addAll(_)

		1 * dependency.artifacts >> artifacts
		1 * expected.artifacts >> mockArtifacts
		1 * mockArtifacts.addAll(_)

		1 * expected.setTransitive(true)
		0 * _
	}
}
