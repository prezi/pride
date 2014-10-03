package com.prezi.gradle.pride.internal;

import com.google.common.base.Strings;
import com.prezi.gradle.pride.Named;
import com.prezi.gradle.pride.Pride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class LoggedProgressAction<T> implements ProgressAction<T> {
	protected static final Logger logger = LoggerFactory.getLogger(LoggedProgressAction.class);

	private final String logPrefix;
	private final String logSuffix;
	private final Named.Namer<? super T> namer;

	public LoggedProgressAction(String logPrefix, Named.Namer<? super T> namer) {
		this(logPrefix, null, namer);
	}

	public LoggedProgressAction(String logPrefix, String logSuffix, Named.Namer<? super T> namer) {
		this.namer = namer;
		this.logPrefix = Strings.isNullOrEmpty(logPrefix) ? "" : logPrefix + " ";
		this.logSuffix = Strings.isNullOrEmpty(logSuffix) ? "" : " " + logSuffix;
	}

	@Override
	public void execute(Pride pride, T item, int index, int count) throws IOException {
		logger.info("[{}/{}] {}{}{}", index + 1, count, logPrefix, namer.getName(item), logSuffix);
		execute(pride, item);
	}

	abstract protected void execute(Pride pride, T item) throws IOException;
}
