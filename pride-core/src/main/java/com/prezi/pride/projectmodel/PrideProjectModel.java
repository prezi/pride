package com.prezi.pride.projectmodel;

import java.util.Map;
import java.util.Set;

public interface PrideProjectModel {
	String getName();
	String getPath();
	String getGroup();
	String getVersion();
	Map<String, Set<DynamicDependency>> getDynamicDependencies();
	String getProjectDir();
	Set<PrideProjectModel> getChildren();
}
