package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class SingleProjectSpec extends Specification {
    static final String PLUGIN_ID = 'com.github.ksoichiro.eclipse.aar'

    def "apply"() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply PLUGIN_ID

        then:
        project.tasks.cleanEclipseDependencies instanceof CleanTask
        project.tasks.generateEclipseDependencies instanceof GenerateTask
        project.extensions.eclipseAar instanceof AarPluginExtension
    }

    def "normalProject"() {
        setup:
        Project project = ProjectBuilder.builder().withProjectDir(new File("src/test/projects/normal")).build()
        ['.gradle', 'userHome', 'aarDependencies', 'libs'].each {
            if (project.file(it).exists()) {
                project.delete(it)
            }
        }
        project.plugins.apply AppPlugin
        project.plugins.apply PLUGIN_ID
        project.repositories { RepositoryHandler it ->
            it.mavenCentral()
            it.maven {
                it.url = project.uri("${System.env.ANDROID_HOME}/extras/android/m2repository")
            }
        }
        project.dependencies {
            compile 'com.android.support:appcompat-v7:21.0.2'
            compile 'com.nineoldandroids:library:2.4.0'
            compile 'com.melnykov:floatingactionbutton:1.0.7'
            compile 'com.github.ksoichiro:android-observablescrollview:1.5.0'
        }

        when:
        project.tasks.generateEclipseDependencies.execute()
        File classpathFile = project.file('.classpath')

        then:
        classpathFile.exists()
        classpathFile.text == """\
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="src" path="src"/>
\t<classpathentry kind="src" path="gen"/>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
\t<classpathentry kind="lib" path="libs/library-2.4.0.jar"/>
\t<classpathentry kind="lib" path="libs/support-annotations-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/appcompat-v7-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/floatingactionbutton-1.0.7.jar"/>
\t<classpathentry kind="lib" path="libs/android-observablescrollview-1.5.0.jar"/>
\t<classpathentry kind="lib" path="libs/support-v4-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/recyclerview-v7-21.0.0.jar"/>
</classpath>
"""
    }
}
