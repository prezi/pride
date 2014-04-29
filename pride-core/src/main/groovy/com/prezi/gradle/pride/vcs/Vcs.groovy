package com.prezi.gradle.pride.vcs

import groovy.transform.TupleConstructor

/**
 * Created by lptr on 29/04/14.
 */
@TupleConstructor
class Vcs {
	String type
	VcsSupport support
}
