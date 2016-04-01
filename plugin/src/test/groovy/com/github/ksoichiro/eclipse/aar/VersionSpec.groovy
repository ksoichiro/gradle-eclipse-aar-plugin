package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class VersionSpec extends BaseSpec {
    @Rule
    TemporaryFolder temporaryFolder

    def "version with rc"() {
        setup:
        Project project = ProjectBuilder
                .builder()
                .withProjectDir(temporaryFolder.root)
                .build()
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
