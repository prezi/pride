package com.prezi.gradle.pride.internal;

import com.prezi.gradle.pride.Pride;

import java.io.IOException;

public interface ProgressAction<T> {
	void execute(Pride pride, T item, int index, int count) throws IOException;
}
