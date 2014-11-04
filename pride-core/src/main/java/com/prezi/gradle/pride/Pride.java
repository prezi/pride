package com.prezi.gradle.pride;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.prezi.gradle.pride.config.ConfigurationData;
import com.prezi.gradle.pride.config.PrideConfigurationHandler;
import com.prezi.gradle.pride.filters.Filter;
import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsManager;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pride {
	private static final Logger logger = LoggerFactory.getLogger(Pride.class);

	public static final String PRIDE_CONFIG_DIRECTORY = ".pride";
	public static final String PRIDE_VERSION_FILE = "version";
	public static final String PRIDE_CONFIG_FILE = "config";
	public static final String PRIDE_PROJECTS_FILE = "projects";

	public static final String GRADLE_SETTINGS_FILE = "settings.gradle";
	public static final String GRADLE_BUILD_FILE = "build.gradle";

	private final File rootDirectory;
	private final File gradleSettingsFile;
	private final File gradleBuildFile;
	private final PropertiesConfiguration localConfiguration;
	private final RuntimeConfiguration configuration;

	private final SortedMap<String, Module> modules;
	private final PrideConfigurationHandler configurationHandler;

	public static Pride getPride(final File directory, RuntimeConfiguration globalConfig, VcsManager vcsManager) throws IOException {
		File prideDirectory = findPrideDirectory(directory);
		if (prideDirectory == null) {
			throw new PrideException("No pride found in " + directory);
		}
		PropertiesConfiguration prideConfig = loadLocalConfiguration(getPrideConfigDirectory(prideDirectory));
		return new Pride(prideDirectory, globalConfig, prideConfig, vcsManager);
	}

	public static File findPrideDirectory(File directory) throws IOException {
		if (containsPride(directory)) {
			return directory;
		} else {
			File parent = directory.getParentFile();
			if (parent != null) {
				return findPrideDirectory(parent);
			} else {
				return null;
			}
		}
	}

	public static boolean containsPride(File directory) {
		File versionFile = new File(new File(directory, PRIDE_CONFIG_DIRECTORY), PRIDE_VERSION_FILE);
		boolean result;
		try {
			result = versionFile.exists() && FileUtils.readFileToString(versionFile).equals("0\n");
		} catch (IOException ex) {
			throw Throwables.propagate(ex);
		}
		logger.debug("Directory " + directory + " contains a pride: " + result);
		return result;
	}

	public Pride(final File rootDirectory, RuntimeConfiguration globalConfiguration, PropertiesConfiguration prideConfiguration, VcsManager vcsManager) throws IOException {
		this.rootDirectory = rootDirectory;
		this.gradleSettingsFile = new File(rootDirectory, GRADLE_SETTINGS_FILE);
		this.gradleBuildFile = new File(rootDirectory, GRADLE_BUILD_FILE);
		this.localConfiguration = prideConfiguration;
		this.configurationHandler = new PrideConfigurationHandler(vcsManager);
		ConfigurationData<Module> configurationData = configurationHandler.loadConfiguration(prideConfiguration);
		this.configuration = globalConfiguration.withConfiguration(configurationData.getConfiguration());
		this.modules = loadModules(rootDirectory, configurationData.getModules());
	}

	private static PropertiesConfiguration loadLocalConfiguration(File configDirectory) {
		File configFile = new File(configDirectory, PRIDE_CONFIG_FILE);
		try {
			if (!configFile.exists()) {
				FileUtils.forceMkdir(configFile.getParentFile());
				//noinspection ResultOfMethodCallIgnored
				configFile.createNewFile();
			}

			return new PropertiesConfiguration(configFile);
		} catch (Exception ex) {
			throw new PrideException("Couldn't load configuration file: " + configFile, ex);
		}
	}

	// Format is: com.example.test:test::test
	private static final Pattern PROJECT_PATTERN = Pattern.compile("(.+?):(.+?):(:.+)");

	public static SortedSet<PrideProjectData> loadProjects(File projectsFile) throws IOException {
		SortedSet<PrideProjectData> projects = Sets.newTreeSet();
		if (projectsFile.exists()) {
			for (String line : Files.asCharSource(projectsFile, Charsets.UTF_8).readLines()) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				Matcher matcher = PROJECT_PATTERN.matcher(line);
				if (!matcher.matches()) {
					throw new IllegalStateException("Cannot read line in " + projectsFile + ": " + line);
				}

				projects.add(new PrideProjectData(matcher.group(1), matcher.group(2), matcher.group(3)));
			}
		}
		return projects;
	}

	public static void saveProjects(File projectsFile, Collection<PrideProjectData> projects) throws IOException {
		FileUtils.deleteQuietly(projectsFile);
		for (PrideProjectData project : projects) {
			FileUtils.write(projectsFile, project.getGroup() + ":" + project.getName() + ":" + project.getPath() + "\n", true);
		}
	}

	/**
	 * Returns the local configuration.
	 *
	 * @return the local configuration.
	 */
	public PropertiesConfiguration getLocalConfiguration() {
		return localConfiguration;
	}

	/**
	 * Returns the merged local and global configurations.
	 *
	 * @return the merged configuration.
	 */
	public RuntimeConfiguration getConfiguration() {
		return configuration;
	}

	public Collection<Module> getModules() {
		return Sets.newTreeSet(modules.values());
	}

	public Module addModule(String name, Vcs vcs) {
		Module module = new Module(name, vcs);
		modules.put(name, module);
		return module;
	}

	public void removeModule(final String name) throws IOException {
		final File moduleDir = getModuleDirectory(name);
		logger.info("Removing " + name + " from " + moduleDir);
		modules.remove(name);
		FileUtils.deleteDirectory(moduleDir);
	}

	public boolean hasModule(String name) {
		return modules.containsKey(name);
	}

	public Module getModule(final String name) {
		if (!modules.containsKey(name)) {
			throw new PrideException("No module with name " + name);
		}

		return modules.get(name);
	}

	public Collection<Module> getModules(final Filter filter) throws IOException {
		SortedSet<Module> filteredModules = Sets.newTreeSet();
		for (Module module : modules.values()) {
			if (filter.matches(this, module)) {
				filteredModules.add(module);
			}
		}
		return filteredModules;
	}

	public File getModuleDirectory(String name) {
		// Do this round-trip to make sure we have the module
		Module module = getModule(name);
		return new File(rootDirectory, module.getName());
	}

	public void save() throws ConfigurationException {
		configurationHandler.saveConfiguration(localConfiguration, modules.values());
		localConfiguration.save();
	}

	private static SortedMap<String, Module> loadModules(File rootDirectory, Collection<Module> modules) throws IOException {
		SortedMap<String, Module> modulesMap = Maps.newTreeMap();
		for (Module module : modules) {
			String moduleName = module.getName();
			File moduleDir = new File(rootDirectory, moduleName);
			if (!moduleDir.isDirectory()) {
				throw new PrideException("Module \"" + moduleName + "\" is missing (" + moduleDir + ")");
			}

			if (!isValidModuleDirectory(moduleDir)) {
				throw new PrideException("No module found in \"" + moduleDir + "\"");
			}

			logger.debug("Found {} module {}", module.getVcs().getType(), moduleName);

			modulesMap.put(module.getName(), module);
		}
		return modulesMap;
	}

	public static boolean isValidModuleDirectory(File dir) {
		if (dir.getName().startsWith(".")) {
			return false;
		}
		for (String fileName : dir.list()) {
			if (GRADLE_BUILD_FILE.equals(fileName) || GRADLE_SETTINGS_FILE.equals(fileName)) {
				return true;
			}
		}
		return false;
	}

	public static File getPrideConfigDirectory(File prideDirectory) {
		return new File(prideDirectory, PRIDE_CONFIG_DIRECTORY);
	}

	public static File getPrideVersionFile(File configDirectory) {
		return new File(configDirectory, PRIDE_VERSION_FILE);
	}

	public static File getPrideConfigFile(File configDirectory) {
		return new File(configDirectory, PRIDE_CONFIG_FILE);
	}

	public static File getPrideProjectsFile(File configDirectory) {
		return new File(configDirectory, PRIDE_PROJECTS_FILE);
	}

	public File getRootDirectory() {
		return rootDirectory;
	}

	public File getGradleSettingsFile() {
		return gradleSettingsFile;
	}

	public File getGradleBuildFile() {
		return gradleBuildFile;
	}
}
