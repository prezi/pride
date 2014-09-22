package com.prezi.gradle.pride.filters;

import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;

import java.io.IOException;

public interface Filter {
	boolean matches(Pride pride, Module module) throws IOException;

	String toString();
}
