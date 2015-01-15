package com.prezi.pride.filters;

import com.prezi.pride.Module;
import com.prezi.pride.Pride;

import java.io.IOException;

public interface Filter {
	boolean matches(Pride pride, Module module) throws IOException;

	String toString();
}
