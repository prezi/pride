package com.prezi.pride.internal;

import com.prezi.pride.Pride;

import java.io.IOException;

public interface ProgressAction<T> {
	void execute(Pride pride, T item, int index, int count) throws IOException;
}
