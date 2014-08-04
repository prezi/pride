package com.prezi.gradle.pride.model;

import java.util.Set;

public interface PrideProjectModel {
	String getName();
	String getPath();
	String getGroup();
	Set<PrideProjectModel> getChildren();
}
