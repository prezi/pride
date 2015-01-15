package com.prezi.pride.internal;

import com.prezi.pride.Named;

public abstract class LoggedNamedProgressAction<T extends Named> extends LoggedProgressAction<T> {
	public LoggedNamedProgressAction(String logPrefix) {
		super(logPrefix, Named.NAMED_NAMER);
	}

	public LoggedNamedProgressAction(String logPrefix, String logSuffix) {
		super(logPrefix, logSuffix, Named.NAMED_NAMER);
	}
}
