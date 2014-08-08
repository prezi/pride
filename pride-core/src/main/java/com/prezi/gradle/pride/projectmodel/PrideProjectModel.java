package com.prezi.gradle.pride.projectmodel;

import java.util.Set;

public interface PrideProjectModel {
	String getName();
	String getPath();
	String getGroup();
	String getProjectDir();
	Set<PrideProjectModel> getChildren();
}
