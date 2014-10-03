package com.prezi.gradle.pride.cli.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.prezi.gradle.pride.Module;
import com.prezi.gradle.pride.Pride;
import com.prezi.gradle.pride.PrideException;
import com.prezi.gradle.pride.RuntimeConfiguration;
import com.prezi.gradle.pride.cli.gradle.GradleConnectorManager;
import com.prezi.gradle.pride.cli.model.ProjectModelAccessor;
import com.prezi.gradle.pride.internal.LoggedNamedProgressAction;
import com.prezi.gradle.pride.internal.ProgressUtils;
import com.prezi.gradle.pride.projectmodel.DynamicDependency;
import com.prezi.gradle.pride.projectmodel.PrideProjectModel;
import io.airlift.command.Arguments;
import io.airlift.command.Command;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Command(name = "check-versions", description = "Check if dependency and module versions match")
public class CheckVersionsCommand extends AbstractFilteredPrideCommand {

	@Arguments(description = "Modules to check")
	private List<String> includeModules;

	@Override
	protected void executeInModules(Pride pride, Collection<Module> modules) throws Exception {
		RuntimeConfiguration config = pride.getConfiguration();
		final ProjectModelAccessor modelAccessor = ProjectModelAccessor.create(new GradleConnectorManager(config), isVerbose());
		final Set<PrideProjectModel> models = Sets.newLinkedHashSet();
		ProgressUtils.execute(pride, modules, new LoggedNamedProgressAction<Module>("Checking dependency versions in module") {
			@Override
			protected void execute(Pride pride, Module module) throws IOException {
				PrideProjectModel rootModel = modelAccessor.getRootProjectModel(pride.getModuleDirectory(module.getName()));
				addWithChildren(rootModel, models);
			}
		});

		Map<String, PrideProjectModel> projectsByGroupAndName = Maps.newTreeMap();
		for (PrideProjectModel model : models) {
			projectsByGroupAndName.put(getId(model), model);
		}
		boolean problem = false;
		for (PrideProjectModel model : models) {
			for (Set<DynamicDependency> dependencies : model.getDynamicDependencies().values()) {
				for (DynamicDependency dependency : dependencies) {
					PrideProjectModel dependencyModel = projectsByGroupAndName.get(getId(dependency));
					if (dependencyModel != null) {
						if (!matchVersion(dependency.getVersion(), dependencyModel.getVersion())) {
							logger.warn("Project \"{}\" requests version {} of project \"{}\", but its current version ({}) does not fulfill that request", getId(model), dependency.getVersion(), getId(dependencyModel), dependencyModel.getVersion());
							problem = true;
						}
					}
				}
			}
		}
		if (problem) {
			throw new PrideException("There are modules that refer to dependency versions that wouldn't work outside this pride. See warnings above.");
		}
	}

	static boolean matchVersion(String requested, String actual) {
		// TODO Make this a little safer
		String regex = requested.replaceAll("\\.", "\\\\.").replaceAll("\\+", ".*");
		Pattern requestedPattern = Pattern.compile(regex);
		return requestedPattern.matcher(actual).matches();
	}

	@Override
	protected Collection<String> getIncludeModules() {
		return null;
	}

	private static void addWithChildren(PrideProjectModel model, Set<PrideProjectModel> models) {
		models.add(model);
		for (PrideProjectModel childModel : model.getChildren()) {
			addWithChildren(childModel, models);
		}
	}

	private static String getId(PrideProjectModel model) {
		return model.getGroup() + ":" + model.getName();
	}

	private static String getId(DynamicDependency dependency) {
		return dependency.getGroup() + ":" + dependency.getName();
	}
}
