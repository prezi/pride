package com.prezi.pride.internal;

import com.prezi.pride.Pride;

import java.io.IOException;
import java.util.Collection;

public class ProgressUtils {
	public static <T> void execute(Pride pride, Collection<? extends T> items, ProgressAction<? super T> action) throws IOException {
		int index = 0;
		for (T item : items) {
			action.execute(pride, item, index, items.size());
			index++;
		}
	}
}
