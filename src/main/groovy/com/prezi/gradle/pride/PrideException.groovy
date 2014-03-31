package com.prezi.gradle.pride

/**
 * Created by lptr on 31/03/14.
 */
class PrideException extends RuntimeException {
	PrideException() {
	}

	PrideException(String s) {
		super(s)
	}

	PrideException(String s, Throwable throwable) {
		super(s, throwable)
	}

	PrideException(Throwable throwable) {
		super(throwable)
	}
}
