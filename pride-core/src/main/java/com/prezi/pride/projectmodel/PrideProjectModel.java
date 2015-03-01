package com.prezi.pride.projectmodel;

import java.util.Set;

public interface PrideProjectModel {
	String getName();
	String getPath();
	String getGroup();
	String getVersion();
	String getProjectDir();
	Set<PrideProjectModel> getChildren();
}
