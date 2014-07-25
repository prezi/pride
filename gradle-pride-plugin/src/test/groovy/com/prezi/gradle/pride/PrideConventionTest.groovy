package com.prezi.gradle.pride

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import spock.lang.Specification

/**
 * Created by lptr on 02/04/14.
 */
class PrideConventionTest extends Specification {
	Project project = Mock()
	DependencyHandler dependencyHandler = Mock()
	PrideConvention convention

	def setup() {
		project.dependencies >> dependencyHandler
		convention = new PrideConvention(project)
	}

	def "self reference"() {
		expect:
		convention.findRelativeProject("") == project
	}

	def "child project reference"() {
		Project tibor = Mock()

		when:
		def result = convention.findRelativeProject("tibor")

		then:
		1 * project.findProject("tibor") >> tibor
		result == tibor
	}

	def "grandchild project reference"() {
		Project lajos = Mock()

		when:
		def result = convention.findRelativeProject("tibor:lajos")

		then:
		1 * project.findProject("tibor:lajos") >> lajos
		result == lajos
	}

	def "sibling project reference"() {
		Project parent = Mock()
		Project tibor = Mock()

		when:
		def result = convention.findRelativeProject(":tibor")

		then:
		1 * project.parent >> parent
		1 * parent.findProject("tibor") >> tibor
		result == tibor
	}

	def "parent project reference"() {
		Project parent = Mock()

		when:
		def result = convention.findRelativeProject(":")

		then:
		1 * project.parent >> parent
		result == parent
	}

	def "grandparent's child project reference"() {
		Project parent = Mock()
		Project grandParent = Mock()
		Project tibor = Mock()

		when:
		def result = convention.findRelativeProject("::tibor")

		then:
		1 * project.parent >> parent
		1 * parent.parent >> grandParent
		1 * grandParent.findProject("tibor") >> tibor
		result == tibor
	}
}
