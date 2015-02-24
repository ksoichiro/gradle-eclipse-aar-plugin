package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import spock.lang.Specification

class BaseSpec extends Specification {
    static final String PLUGIN_ID = 'com.github.ksoichiro.eclipse.aar'

    void deleteOutputs(Project project) {
        ['.gradle', 'userHome', 'aarDependencies', 'libs', '.classpath', '.project', 'project.properties'].each {
            if (project.file(it).exists()) {
                project.delete(it)
            }
        }
    }

    void setupRepositories(Project project) {
        project.repositories { RepositoryHandler it ->
            it.mavenCentral()
            it.maven {
                it.url = project.uri("${System.env.ANDROID_HOME}/extras/android/m2repository")
            }
        }
    }
}
