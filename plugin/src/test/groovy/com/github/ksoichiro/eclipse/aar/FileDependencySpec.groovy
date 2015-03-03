package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.testfixtures.ProjectBuilder

import java.util.regex.Matcher

class FileDependencySpec extends BaseSpec {

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
    }

    void deleteOutputs(Project project) {
        ['.gradle', 'userHome', 'aarDependencies', '.classpath', '.project', 'project.properties',
         'com.android.support-appcompat-v7-21.0.2.jar', 'com.android.support-support-annotations-21.0.2.jar',
         'com.android.support-support-v4-21.0.2.jar'].each {
            if (project.file(it).exists()) {
                project.delete(it)
            }
        }
    }

    Project setupProject(List<String> libs) {
        Project project = ProjectBuilder.builder().withProjectDir(new File("src/test/projects/file")).build()
        deleteOutputs(project)
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
