package org.springframework.build.gradle.springio.platform

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
class AlternativeDependenciesTask extends DefaultTask {
	@OutputFile
	File reportFile = project.file("$project.buildDir/springio/alternative-dependencies.log")
	Map<String,String> alternatives

	@TaskAction
	void check() {
		reportFile.parentFile.mkdirs()
		if(!alternatives) {
			InputStream stream = getClass().getResourceAsStream('springio-alternatives.properties')
			alternatives = new Properties()
			alternatives.load(stream)
		}

		def problemsByConfiguration = [:]
		project.configurations.each { configuration ->
			def problems = []
			configuration.dependencies.each { dependency ->
				if (dependency instanceof ExternalModuleDependency) {
					def groupId = dependency.group
					def artifactId = dependency.name
					def version = dependency.version
					def problem = checkDependency(dependency.group, dependency.name, dependency.version)
					if (problem) {
						problems << problem
					}
				}
			}
			if (problems) {
				problemsByConfiguration[configuration.name] = problems
			}
		}
		if (problemsByConfiguration) {
			reportFile.withWriterAppend { out ->
				out.writeLine(project.name)
				problemsByConfiguration.each { configuration, problems ->
					out.writeLine("    Configuration: ${configuration}")
					problems.each { out.writeLine("        ${it}") }
				}
			}
			throw new IllegalStateException("Found dependencies that have better alternatives. See $reportFile for a detailed report")
		}
	}

	def checkDependency(groupId, artifactId, version) {
		def id = "${groupId}:${artifactId}"
		def alternative = alternatives[id]

		if (alternative) {
			"Please depend on ${alternative} instead of ${id}"
		}
	}
}