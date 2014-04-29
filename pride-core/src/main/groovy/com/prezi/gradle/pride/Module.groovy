package com.prezi.gradle.pride

import com.prezi.gradle.pride.vcs.Vcs
import groovy.transform.TupleConstructor

/**
 * Created by lptr on 29/04/14.
 */
@TupleConstructor
class Module implements Comparable<Module> {
	String name
	Vcs vcs

	@Override
	int compareTo(Module o) {
		return name.compareTo(o.name)
	}
}
