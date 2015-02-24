package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.testfixtures.ProjectBuilder

class VersionSpec extends BaseSpec {

    def "version with rc"() {
        setup:
        Project project = ProjectBuilder.builder().withProjectDir(new File("src/test/projects/version")).build()
        deleteOutputs(project)
        project.plugins.apply AppPlugin
        project.plugins.apply PLUGIN_ID
        setupRepositories(project)
        project.dependencies {
            compile 'com.bingzer.android.driven:driven-gdrive:1.0.0'
        }
        project.android.sourceSets.main.java.srcDirs = [ 'src' ]

        when:
        project.tasks.generateEclipseDependencies.execute()
        File classpathFile = project.file('.classpath')
        File projectPropertiesFile = project.file('project.properties')

        then:
        classpathFile.exists()
        projectPropertiesFile.exists()
    }
}
