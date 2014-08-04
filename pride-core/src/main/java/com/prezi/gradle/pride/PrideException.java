package com.prezi.gradle.pride;

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
