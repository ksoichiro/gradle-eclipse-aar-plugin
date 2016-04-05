package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class MultiProjectSpec extends BaseSpec {
    @Rule
    TemporaryFolder temporaryFolder

    def "multiple projects"() {
        setup:
        def (project, projectLibrary, projectApp) = setupMultiProject()

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
        File projectLibraryFile = projectLibrary.file('.project')
        File projectAppFile = projectApp.file('.project')
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
</classpath>
"""
        projectLibraryFile.exists()
        projectAppFile.exists()
        projectPropertiesLibraryFile.exists()
        projectPropertiesAppFile.exists()
        projectPropertiesLibraryFile.text == """target=android-21
android.library=true
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

    def "applying plugin to the sub project"() {
        setup:
        def builder = ProjectBuilder.builder()
        Project project = builder.withProjectDir(temporaryFolder.root).build()
        Project projectLibrary = builder
                .withProjectDir(temporaryFolder.newFolder('library'))
                .withParent(project)
                .withName(':library')
                .build()
        Project projectApp = builder
                .withProjectDir(temporaryFolder.newFolder('app'))
                .withParent(project)
                .withName(':app')
                .build()

        project.subprojects.each { Project p ->
            setupRepositories(p)
            ['libs', 'src/main/java'].collect { p.file(it) }*.mkdirs()
        }

        projectLibrary.file('src/main/AndroidManifest.xml').text = """<manifest>
</manifest>
"""

        projectLibrary.plugins.apply LibraryPlugin
        projectApp.plugins.apply AppPlugin

        // Same effect as applying the plugin to the root project
        projectApp.plugins.apply PLUGIN_ID

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
        projectApp.tasks.generateEclipseDependencies.execute()
        File classpathLibraryFile = projectLibrary.file('.classpath')
        File classpathAppFile = projectApp.file('.classpath')
        File projectLibraryFile = projectLibrary.file('.project')
        File projectAppFile = projectApp.file('.project')
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
</classpath>
"""
        projectLibraryFile.exists()
        projectAppFile.exists()
        projectPropertiesLibraryFile.exists()
        projectPropertiesAppFile.exists()
        projectPropertiesLibraryFile.text == """target=android-21
android.library=true
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

    def "multiple projects that has meta data files"() {
        setup:
        def (project, projectLibrary, projectApp) = setupMultiProject()

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
        File projectPropertiesLibraryFile = projectLibrary.file('project.properties')
        projectPropertiesLibraryFile.text = """target=android-21
android.library=true
android.library.reference.1=aarDependencies/com.android.support-recyclerview-v7-21.0.0
android.library.reference.2=aarDependencies/com.android.support-support-v4-21.0.2
"""
        File projectPropertiesAppFile = projectApp.file('project.properties')
        projectPropertiesAppFile.text = """target=android-21
android.library.reference.1=aarDependencies/com.android.support-appcompat-v7-21.0.2
android.library.reference.2=aarDependencies/com.melnykov-floatingactionbutton-1.0.7
android.library.reference.3=aarDependencies/com.android.support-support-v4-21.0.2
android.library.reference.4=aarDependencies/com.android.support-recyclerview-v7-21.0.0
android.library.reference.5=../library
"""

        when:
        project.tasks.generateEclipseDependencies.execute()
        File classpathLibraryFile = projectLibrary.file('.classpath')
        File classpathAppFile = projectApp.file('.classpath')
        File projectLibraryFile = projectLibrary.file('.project')
        File projectAppFile = projectApp.file('.project')

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
</classpath>
"""
        projectLibraryFile.exists()
        projectAppFile.exists()
        projectPropertiesLibraryFile.exists()
        projectPropertiesAppFile.exists()
        projectPropertiesLibraryFile.text == """target=android-21
android.library=true
android.library.reference.1=aarDependencies/com.android.support-recyclerview-v7-21.0.0
android.library.reference.2=aarDependencies/com.android.support-support-v4-21.0.2
"""
        projectPropertiesAppFile.text == """target=android-21
android.library.reference.1=aarDependencies/com.android.support-appcompat-v7-21.0.2
android.library.reference.2=aarDependencies/com.melnykov-floatingactionbutton-1.0.7
android.library.reference.3=aarDependencies/com.android.support-support-v4-21.0.2
android.library.reference.4=aarDependencies/com.android.support-recyclerview-v7-21.0.0
android.library.reference.5=../library
"""
    }

    def "duplicate dependencies with different version"() {
        setup:
        def (project, projectLibrary, projectApp) = setupMultiProject()

        projectLibrary.dependencies {
            compile 'com.android.support:recyclerview-v7:21.0.0'
            compile 'com.android.support:appcompat-v7:21.0.0'
            compile 'com.nineoldandroids:library:2.4.0'
        }
        projectLibrary.android {
            compileSdkVersion 1 // Whatever, but required
            sourceSets.main.java.srcDirs = [ 'src/main/java' ]
        }
        projectApp.dependencies { DependencyHandler it ->
            compile 'com.android.support:appcompat-v7:21.0.2'
            compile 'com.melnykov:floatingactionbutton:1.0.7'
            compile 'com.android.support:support-v4:21.0.0'
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
        File projectLibraryFile = projectLibrary.file('.project')
        File projectAppFile = projectApp.file('.project')
        File projectPropertiesLibraryFile = projectLibrary.file('project.properties')
        File projectPropertiesAppFile = projectApp.file('project.properties')

        then:
        classpathLibraryFile.exists()
        classpathAppFile.exists()
        projectLibraryFile.exists()
        projectAppFile.exists()
        projectPropertiesLibraryFile.exists()
        projectPropertiesAppFile.exists()
    }

    def setupMultiProject() {
        def builder = ProjectBuilder.builder()
        Project project = builder.withProjectDir(temporaryFolder.root).build()
        Project projectLibrary = builder
                .withProjectDir(temporaryFolder.newFolder('library'))
                .withParent(project)
                .withName(':library')
                .build()
        Project projectApp = builder
                .withProjectDir(temporaryFolder.newFolder('app'))
                .withParent(project)
                .withName(':app')
                .build()

        project.subprojects.each { Project p ->
            setupRepositories(p)
            ['libs', 'src/main/java'].collect { p.file(it) }*.mkdirs()
        }

        projectLibrary.file('src/main/AndroidManifest.xml').text = """<manifest>
</manifest>
"""

        projectLibrary.plugins.apply LibraryPlugin
        projectApp.plugins.apply AppPlugin

        project.plugins.apply PLUGIN_ID

        [project, projectLibrary, projectApp]
    }
}
