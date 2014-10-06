package com.prezi.gradle.pride.cli.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import com.prezi.gradle.pride.cli.ivyversions.ResolverStrategy;
import com.prezi.gradle.pride.cli.model.ProjectModelAccessor;
import com.prezi.gradle.pride.projectmodel.DynamicDependency;
import com.prezi.gradle.pride.projectmodel.PrideProjectModel;
import io.airlift.command.Command;

import java.util.Map;
import java.util.Set;

@Command(name = "check-versions", description = "Check if dependency and project versions match")
public class CheckVersionsCommand extends AbstractPrideCommand {

	@Override
	public void executeInPride(Pride pride) throws Exception {
		RuntimeConfiguration config = pride.getConfiguration();
		final ProjectModelAccessor modelAccessor = ProjectModelAccessor.create(new GradleConnectorManager(config), isVerbose());
		final Set<PrideProjectModel> models = Sets.newLinkedHashSet();
		PrideProjectModel prideModel = modelAccessor.getRootProjectModel(pride.getRootDirectory());
		addChildModels(prideModel, models);

		Map<String, PrideProjectModel> projectsByGroupAndName = Maps.newTreeMap();
		for (PrideProjectModel model : models) {
			projectsByGroupAndName.put(getId(model), model);
		}
		Set<String> versionConflicts = Sets.newLinkedHashSet();
		for (PrideProjectModel model : models) {
			for (Set<DynamicDependency> dependencies : model.getDynamicDependencies().values()) {
				for (DynamicDependency dependency : dependencies) {
					PrideProjectModel dependencyModel = projectsByGroupAndName.get(getId(dependency));
					if (dependencyModel != null) {
						if (!matchVersion(dependency.getVersion(), dependencyModel.getVersion())) {
							String conflict = String.format("Project \"%s\" requests version %s of project \"%s\", but its current version (%s) does not fulfill that request", getId(model), dependency.getVersion(), getId(dependencyModel), dependencyModel.getVersion());
							versionConflicts.add(conflict);
						}
					}
				}
			}
		}
		if (!versionConflicts.isEmpty()) {
			for (String conflict : versionConflicts) {
				logger.warn(conflict);
			}
			throw new PrideException("There are modules that refer to dependency versions that wouldn't work outside this pride. See warnings above.");
		} else {
			logger.info("All projects refer to dependency versions that will resolve to the ones in the pride");
		}
	}

	static boolean matchVersion(String requested, String actual) {
		return new ResolverStrategy().accept(requested, actual);
	}

	private static void addChildModels(PrideProjectModel model, Set<PrideProjectModel> models) {
		for (PrideProjectModel childModel : model.getChildren()) {
			models.add(childModel);
			addChildModels(childModel, models);
		}
	}

	private static String getId(PrideProjectModel model) {
		return model.getGroup() + ":" + model.getName();
	}

	private static String getId(DynamicDependency dependency) {
		return dependency.getGroup() + ":" + dependency.getName();
	}
}
