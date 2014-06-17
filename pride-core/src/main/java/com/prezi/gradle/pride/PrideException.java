package com.prezi.gradle.pride;

/**
 * This exception has been converted to a Java class, because
 * if it's a Groovy class, Pride will throw an ugly exception:
 * https://github.com/prezi/pride/issues/47
 *
 * Created by lptr on 31/03/14.
 */
public class PrideException extends RuntimeException {
	public PrideException()
	{
	}

	public PrideException(String s)
	{
		super(s);
	}

	public PrideException(String s, Throwable throwable)
	{
		super(s, throwable);
	}

	public PrideException(Throwable throwable)
	{
		super(throwable);
	}
}
