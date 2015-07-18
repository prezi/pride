package com.prezi.pride.test
import com.google.common.base.Charsets
import com.google.common.io.Files
import com.google.common.io.Resources
import org.apache.commons.io.FileUtils
import spock.lang.Specification

class AbstractIntegrationSpec extends Specification {
	File rootDir
	File projectDir
	File buildDir
	File prideInstallationDir
	File dir
	String defaultGradleVersion = "2.5"

	def setup() {
		def testName = this.getClass().simpleName

		def projectLocations = asProps(Resources.toString(Resources.getResource("project-locations.properties"), Charsets.UTF_8))
		rootDir = new File(projectLocations.rootDir as String)
		projectDir = new File(projectLocations.projectDir as String)
		buildDir = new File(projectLocations.buildDir as String)
		prideInstallationDir = new File(projectLocations.prideInstallationDir as String)
		dir = new File(buildDir, "integration-tests/${testName}")
	}

	File file(Object path) {
		if (path instanceof File) {
			return path
		}
		return new File(dir, path as String)
	}

	def setupFile(String path) {
		def dest = file(path)
		FileUtils.forceMkdir(dest.parentFile)
		Resources.asByteSource(Resources.getResource(path)).copyTo(Files.asByteSink(dest))
		return dest
	}

	Properties asProps(String contents) {
		def props = new Properties()
		props.load(new StringReader(contents))
		return props
	}

	def asProps(File f) {
		def props = new Properties()
		f.withInputStream {
			props.load(it)
		}
		return props
	}

	def rawContents(Object f, String comment) {
		"\n" + file(f).text.split("\n").findAll { !it.isEmpty() && !it.startsWith(comment) }.join("\n") + "\n"
	}

	Process exec(Map<String, ?> options, Object... commandLine) {
		return exec(options, Arrays.asList(commandLine))
	}

	Process exec(Map<String, ?> options = [:], List<?> commandLine, Closure check = null) {
		File workingDir = options.workingDir ? file(options.workingDir) : dir
		println "Running ${commandLine.join(" ")} in ${workingDir}"
		Process process = commandLine.execute((String[]) null, workingDir)
		if (!check) {
			process.waitForProcessOutput((OutputStream) System.out, System.err)
			assert process.exitValue() == 0
		} else {
			check(process)
		}
		return process
	}

	Process pride(Map<String, ?> options = [:], Object... arguments) {
		return pride(options, arguments as List)
	}

	Process pride(List<?> arguments, Closure check = null) {
		pride [:], arguments, check
	}

	Process pride(Map<String, ?> options, List<?> arguments, Closure check = null) {
		def commandLine = ["$prideInstallationDir/bin/pride", *arguments]
		exec options, commandLine, check
	}

	Process gradle(Map<String, ?> options = [:], Object... arguments) {
		return gradle(options, arguments as List)
	}

	Process gradle(List<?> arguments, Closure check = null) {
		gradle [:], arguments, check
	}
	Process gradle(Map<String, ?> options, List<?> arguments, Closure check = null) {
		def commandLine = ["$rootDir/gradlew", *arguments]
		exec options, commandLine, check
	}
}
