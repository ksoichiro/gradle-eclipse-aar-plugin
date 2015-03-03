package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import spock.lang.Specification

import java.util.regex.Matcher

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

    List<String> jarEntriesFromClasspathFiles(Project project) {
        File classpathFile = project.file('.classpath')
        def classPaths = new XmlSlurper().parseText(classpathFile.text)
        def libClassPathEntries = classPaths.classpathentry?.findAll { it.@kind?.text() == 'lib' }
        libClassPathEntries.collect { it.@path.text().replaceFirst('^libs/', '') }
    }

    List<String> aarEntriesFromProjectProperties(Project project) {
        File projectPropertiesFile = project.file('project.properties')
        Properties props = new Properties()
        projectPropertiesFile.withInputStream { stream -> props.load(stream) }
        List<String> aarNames = []
        props.propertyNames().findAll {
            it =~ /^android\.library\.reference\.[0-9]+/
        }.each {
            Matcher mValue = props[it] =~ /^${project.extensions.eclipseAar.aarDependenciesDir}\\/(.*)/
            if (mValue.matches()) {
                aarNames << mValue[0][1]
            }
        }
        aarNames
    }
}
