package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FileDependencySpec extends BaseSpec {
    @Rule
    TemporaryFolder temporaryFolder

    def "File dependency in libs directory"() {
        setup:
        Project project = setupProject([
                'com.android.support:appcompat-v7:21.0.2',
        ])
        List<String> resolvedJars = [
                'com.android.support-appcompat-v7-21.0.2.jar',
                'com.android.support-support-v4-21.0.2.jar',
                'com.android.support-support-annotations-21.0.2.jar',
        ]
        List<String> resolvedAars = [
                'com.android.support-appcompat-v7-21.0.2',
                'com.android.support-support-v4-21.0.2',
        ]
        setupRepositories(project)
        project.dependencies { DependencyHandler dh ->
            dh.add('compile', project.fileTree(dir: 'libs', includes: ['*.jar']))
        }
        project.extensions.eclipseAar.cleanLibsDirectoryEnabled = false

        when:
        project.tasks.generateEclipseDependencies.execute()
        List<String> jarNames = jarEntriesFromClasspathFiles(project)
        List<String> aarNames = aarEntriesFromProjectProperties(project)

        then:
        jarNames.find { !(it in resolvedJars) } == null
        aarNames.find { !(it in resolvedAars) } == null
        project.file('libs/misc.jar').exists()
    }

    Project setupProject(List<String> libs) {
        addStaticJarFileTo(temporaryFolder.newFolder('libs'))
        Project project = ProjectBuilder.builder().withProjectDir(temporaryFolder.root).build()
        project.plugins.apply AppPlugin
        project.plugins.apply PLUGIN_ID
        setupRepositories(project)
        project.dependencies { DependencyHandler dh ->
            libs.each {
                dh.add('compile', it)
            }
        }
        project
    }
}
