package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.testfixtures.ProjectBuilder

class SingleProjectSpec extends BaseSpec {

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
        ['.gradle', 'userHome', 'aarDependencies', 'libs', '.classpath', 'project.properties'].each {
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
        File projectPropertiesFile = project.file('project.properties')

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
\t<classpathentry kind="lib" path="libs/com.nineoldandroids-library-2.4.0.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-support-annotations-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-appcompat-v7-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.melnykov-floatingactionbutton-1.0.7.jar"/>
\t<classpathentry kind="lib" path="libs/com.github.ksoichiro-android-observablescrollview-1.5.0.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-support-v4-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-recyclerview-v7-21.0.0.jar"/>
</classpath>
"""
        projectPropertiesFile.exists()
        projectPropertiesFile.text == """\
target=android-21
android.library.reference.1=aarDependencies/com.android.support-appcompat-v7-21.0.2
android.library.reference.2=aarDependencies/com.melnykov-floatingactionbutton-1.0.7
android.library.reference.3=aarDependencies/com.github.ksoichiro-android-observablescrollview-1.5.0
android.library.reference.4=aarDependencies/com.android.support-support-v4-21.0.2
android.library.reference.5=aarDependencies/com.android.support-recyclerview-v7-21.0.0
"""
    }

    def "metaFilesExists"() {
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
        File classpathFile = project.file('.classpath')
        classpathFile.text = """\
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="src" path="src"/>
\t<classpathentry kind="src" path="gen"/>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
\t<classpathentry kind="lib" path="libs/com.nineoldandroids-library-2.4.0.jar"/>
</classpath>
"""
        File projectPropertiesFile = project.file('project.properties')
        projectPropertiesFile.text = """\
target=android-21
android.library.reference.1=aarDependencies/com.android.support-appcompat-v7-21.0.2
"""

        when:
        project.tasks.generateEclipseDependencies.execute()

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
\t<classpathentry kind="lib" path="libs/com.nineoldandroids-library-2.4.0.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-support-annotations-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-appcompat-v7-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.melnykov-floatingactionbutton-1.0.7.jar"/>
\t<classpathentry kind="lib" path="libs/com.github.ksoichiro-android-observablescrollview-1.5.0.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-support-v4-21.0.2.jar"/>
\t<classpathentry kind="lib" path="libs/com.android.support-recyclerview-v7-21.0.0.jar"/>
</classpath>
"""
        projectPropertiesFile.exists()
        projectPropertiesFile.text == """\
target=android-21
android.library.reference.1=aarDependencies/com.android.support-appcompat-v7-21.0.2
android.library.reference.2=aarDependencies/com.melnykov-floatingactionbutton-1.0.7
android.library.reference.3=aarDependencies/com.github.ksoichiro-android-observablescrollview-1.5.0
android.library.reference.4=aarDependencies/com.android.support-support-v4-21.0.2
android.library.reference.5=aarDependencies/com.android.support-recyclerview-v7-21.0.0
"""
    }
}
