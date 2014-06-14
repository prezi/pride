package com.prezi.gradle.pride;

import com.prezi.gradle.pride.vcs.Vcs;
import com.prezi.gradle.pride.vcs.VcsManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Pride {
	private static final Logger logger = LoggerFactory.getLogger(Pride.class);
	public static final String PRIDE_CONFIG_DIRECTORY = ".pride";
	public static final String PRIDE_MODULES_FILE = "modules";
	public static final String PRIDE_VERSION_FILE = "version";
	public static final String GRADLE_SETTINGS_FILE = "settings.gradle";
	public static final String GRADLE_BUILD_FILE = "build.gradle";
	public final File rootDirectory;
	public final File configDirectory;
	public final File gradleSettingsFile;
	public final File gradleBuildFile;
	private final SortedMap<String, Module> modules;

	public static Pride lookupPride(File directory, Configuration configuration, VcsManager vcsManager) throws IOException {
		if (containsPride(directory)) {
			return new Pride(directory, configuration, vcsManager);
		} else {
			File parent = directory.getParentFile();
			if (parent != null) {
				return lookupPride(parent, configuration, vcsManager);
			} else {
				return null;
			}
		}
	}

	public static Pride getPride(final File directory, Configuration configuration, VcsManager vcsManager) throws IOException {
		Pride pride = lookupPride(directory, configuration, vcsManager);
		if (pride == null) {
			throw new PrideException("No pride found in " + directory);
		}

		return pride;
	}

	public static boolean containsPride(final File directory) throws IOException {
		File versionFile = new File(new File(directory, PRIDE_CONFIG_DIRECTORY), PRIDE_VERSION_FILE);
		boolean result = versionFile.exists() && FileUtils.readFileToString(versionFile).equals("0\n");
		logger.debug("Directory " + directory + " contains a pride: " + result);
		return result;
	}

	private Pride(final File rootDirectory, Configuration configuration, VcsManager vcsManager) throws IOException {
		this.rootDirectory = rootDirectory;
		this.configDirectory = getPrideConfigDirectory(rootDirectory);
		this.gradleSettingsFile = new File(rootDirectory, GRADLE_SETTINGS_FILE);
		this.gradleBuildFile = new File(rootDirectory, GRADLE_BUILD_FILE);
		if (!configDirectory.isDirectory()) {
			throw new PrideException("No pride in directory \"" + rootDirectory + "\"");
		}

		this.modules = loadModules(rootDirectory, getPrideModulesFile(configDirectory), configuration, vcsManager);
	}

	public Collection<Module> getModules() {
		return Collections.unmodifiableCollection(modules.values());
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

	public File getModuleDirectory(String name) {
		// Do this round-trip to make sure we have the module
		Module module = getModule(name);
		return new File(rootDirectory, module.name);
	}

	public void save() throws IOException {
		final File modulesFile = getPrideModulesFile(configDirectory);
		modulesFile.delete();
		modulesFile.createNewFile();
		for (Module module : modules.values()) {
			FileUtils.write(modulesFile, module.vcs.getType() + "|" + module.name + "\n", true);
		}
	}

	private static SortedMap<String, Module> loadModules(final File rootDirectory, final File modulesFile, final Configuration configuration, final VcsManager vcsManager) throws IOException {
		if (!modulesFile.exists()) {
			throw new PrideException("Cannot find modules file at " + modulesFile);
		}

		List<Module> modules = new ArrayList<Module>();
		for (String line : FileUtils.readLines(modulesFile)) {
			String moduleLine = line.trim();
			if (moduleLine.isEmpty() || moduleLine.startsWith("#")) {
				continue;
			}

			String moduleName;
			String vcsType;
			Matcher matcher = Pattern.compile("(.*)?\\|(.*)").matcher(moduleLine);
			if (matcher.matches()) {
				vcsType = matcher.group(1);
				moduleName = matcher.group(2);
			} else {
				// Default to git for backwards compatibility
				vcsType = "git";
				moduleName = moduleLine;
			}

			final File moduleDir = new File(rootDirectory, moduleName);
			if (!moduleDir.isDirectory()) {
				throw new PrideException("Module \"" + moduleName + "\" is missing (" + moduleDir +")");
			}

			if (!isValidModuleDirectory(moduleDir)) {
				throw new PrideException("No module found in \"" + moduleDir + "\"");
			}

			logger.debug("Found {} module {}", vcsType, moduleName);

			Module module = new Module(moduleName, vcsManager.getVcs(vcsType, configuration));
			modules.add(module);
		}
		TreeMap<String, Module> modulesMap = new TreeMap<String, Module>();
		for (Module module : modules) {
			modulesMap.put(module.name, module);
		}
		return modulesMap;
	}

	public static boolean isValidModuleDirectory(File dir) {
		return !dir.getName().startsWith(".") && ArrayUtils.contains(dir.list(), GRADLE_BUILD_FILE);
	}

	public static File getPrideConfigDirectory(File prideDirectory) {
		return new File(prideDirectory, PRIDE_CONFIG_DIRECTORY);
	}

	public static File getPrideModulesFile(File configDirectory) {
		return new File(configDirectory, PRIDE_MODULES_FILE);
	}

	public static File getPrideVersionFile(File configDirectory) {
		return new File(configDirectory, PRIDE_VERSION_FILE);
	}
}
