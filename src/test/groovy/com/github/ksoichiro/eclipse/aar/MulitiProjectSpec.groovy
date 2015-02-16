package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.testfixtures.ProjectBuilder

class MulitiProjectSpec extends BaseSpec {
    def "multiple projects"() {
        setup:
        def builder = ProjectBuilder.builder()
        Project project = builder.withProjectDir(new File("src/test/projects/multi")).build()
        Project projectLibrary = builder
                .withProjectDir(new File("src/test/projects/multi/library"))
                .withParent(project)
                .withName(':library')
                .build()
        Project projectApp = builder
                .withProjectDir(new File("src/test/projects/multi/app"))
                .withParent(project)
                .withName(':app')
                .build()

        project.subprojects.each { Project p ->
            ['.gradle', 'userHome', 'aarDependencies', 'libs', '.classpath', 'project.properties'].each {
                if (p.file(it).exists()) {
                    p.delete(it)
                }
            }
        }

        project.subprojects*.repositories { RepositoryHandler it ->
            it.mavenCentral()
            it.maven {
                it.url = project.uri("${System.env.ANDROID_HOME}/extras/android/m2repository")
            }
        }

        projectLibrary.plugins.apply LibraryPlugin
        projectApp.plugins.apply AppPlugin

        project.plugins.apply PLUGIN_ID

        projectLibrary.dependencies {
            compile 'com.android.support:recyclerview-v7:21.0.0'
            androidTestCompile ('com.android.support:appcompat-v7:21.0.2') {
                exclude module: 'support-v4'
            }
            androidTestCompile ('com.nineoldandroids:library:2.4.0') {
                exclude module: 'support-v4'
            }
        }
        projectLibrary.android {
            compileSdkVersion 1 // Whatever, but required
            sourceSets.main.java.srcDirs = [ 'src/main/java' ]
        }
        projectApp.dependencies { DependencyHandler it ->
            compile 'com.android.support:appcompat-v7:21.0.2'
            compile 'com.nineoldandroids:library:2.4.0'
            compile 'com.melnykov:floatingactionbutton:1.0.7'
            debugCompile projectApp.project(':library')
        }
        projectApp.android {
            compileSdkVersion 1 // Whatever, but required
            sourceSets.main.java.srcDirs = [ 'src/main/java' ]
        }

        when:
        project.tasks.generateEclipseDependencies.execute()
        File classpathLibraryFile = projectLibrary.file('.classpath')
        File classpathAppFile = projectApp.file('.classpath')
        File projectPropertiesLibraryFile = projectLibrary.file('project.properties')
        File projectPropertiesAppFile = projectApp.file('project.properties')

        then:
        classpathLibraryFile.exists()
        classpathAppFile.exists()
        classpathLibraryFile.text == """<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="src" path="src/main/java"/>
\t<classpathentry kind="src" path="gen"/>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
\t<classpathentry kind="lib" path="libs/com.android.support-support-annotations-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-recyclerview-v7-21.0.0.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-support-v4-21.0.2.jar"/>
</classpath>
"""
        classpathAppFile.text == """<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="src" path="src/main/java"/>
\t<classpathentry kind="src" path="gen"/>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
\t<classpathentry kind="lib" path="libs/com.nineoldandroids-library-2.4.0.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-support-annotations-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-appcompat-v7-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.melnykov-floatingactionbutton-1.0.7.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-support-v4-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-recyclerview-v7-21.0.0.jar"/>
</classpath>
"""
        projectPropertiesLibraryFile.exists()
        projectPropertiesAppFile.exists()
        projectPropertiesLibraryFile.text == """target=android-21
android.library.reference.1=aarDependencies/com.android.support-recyclerview-v7-21.0.0
android.library.reference.2=aarDependencies/com.android.support-support-v4-21.0.2
"""
        projectPropertiesAppFile.text == """target=android-21
android.library.reference.1=../library
android.library.reference.2=aarDependencies/com.android.support-appcompat-v7-21.0.2
android.library.reference.3=aarDependencies/com.melnykov-floatingactionbutton-1.0.7
android.library.reference.4=aarDependencies/com.android.support-support-v4-21.0.2
android.library.reference.5=aarDependencies/com.android.support-recyclerview-v7-21.0.0
"""
    }
}
