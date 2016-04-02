package com.github.ksoichiro.eclipse.aar

import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class SingleProjectSpec extends BaseSpec {
    @Rule
    TemporaryFolder temporaryFolder

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
        Project project = ProjectBuilder
                .builder()
                .withProjectDir(temporaryFolder.root)
                .withName('normal')
                .build()
        project.plugins.apply AppPlugin
        project.plugins.apply PLUGIN_ID
        setupRepositories(project)
        project.dependencies {
            compile 'com.android.support:appcompat-v7:21.0.2'
            compile 'com.nineoldandroids:library:2.4.0'
            compile 'com.melnykov:floatingactionbutton:1.0.7'
            compile 'com.github.ksoichiro:android-observablescrollview:1.5.0'
        }
        temporaryFolder.newFile('src')
        project.android.sourceSets.main.java.srcDirs = [ 'src' ]

        when:
        project.tasks.generateEclipseDependencies.execute()
        File classpathFile = project.file('.classpath')
        File projectFile = project.file('.project')
        File projectPropertiesFile = project.file('project.properties')

        then:
        classpathFile.exists()
        classpathFile.text == """<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="src" path="src"/>
\t<classpathentry kind="src" path="gen"/>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
</classpath>
"""
        projectFile.exists()
        projectFile.text == """<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
\t<name>${project.name}</name>
\t<comment></comment>
\t<projects>
\t</projects>
\t<buildSpec>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ResourceManagerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.PreCompilerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ApkBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t</buildSpec>
\t<natures>
\t\t<nature>com.android.ide.eclipse.adt.AndroidNature</nature>
\t\t<nature>org.eclipse.jdt.core.javanature</nature>
\t</natures>
</projectDescription>
"""
        projectPropertiesFile.exists()
        projectPropertiesFile.text == """target=android-21
android.library.reference.1=aarDependencies/com.android.support-appcompat-v7-21.0.2
android.library.reference.2=aarDependencies/com.melnykov-floatingactionbutton-1.0.7
android.library.reference.3=aarDependencies/com.github.ksoichiro-android-observablescrollview-1.5.0
android.library.reference.4=aarDependencies/com.android.support-support-v4-21.0.2
android.library.reference.5=aarDependencies/com.android.support-recyclerview-v7-21.0.0
"""
    }

    def "metaFilesExists"() {
        setup:
        Project project = ProjectBuilder
                .builder()
                .withProjectDir(temporaryFolder.root)
                .withName('normal')
                .build()
        project.plugins.apply AppPlugin
        project.plugins.apply PLUGIN_ID
        setupRepositories(project)
        project.dependencies {
            compile 'com.android.support:appcompat-v7:21.0.2'
            compile 'com.nineoldandroids:library:2.4.0'
            compile 'com.melnykov:floatingactionbutton:1.0.7'
            compile 'com.github.ksoichiro:android-observablescrollview:1.5.0'
        }
        temporaryFolder.newFile('src')
        project.android.sourceSets.main.java.srcDirs = [ 'src' ]
        File classpathFile = project.file('.classpath')
        classpathFile.text = """<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
</classpath>
"""
        File projectFile = project.file('.project')
        projectFile.text = """<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
\t<name>${project.name}</name>
\t<comment></comment>
\t<projects>
\t</projects>
\t<buildSpec>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ResourceManagerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.PreCompilerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ApkBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t</buildSpec>
\t<natures>
\t\t<nature>com.android.ide.eclipse.adt.AndroidNature</nature>
\t\t<nature>org.eclipse.jdt.core.javanature</nature>
\t</natures>
</projectDescription>
"""
        File projectPropertiesFile = project.file('project.properties')
        projectPropertiesFile.text = """\
target=android-21
android.library.reference.1=externals/my.lib-1.0.0
android.library.reference.2=aarDependencies/com.android.support-appcompat-v7-21.0.2
"""

        when:
        project.tasks.generateEclipseDependencies.execute()

        then:
        classpathFile.exists()
        classpathFile.text == """<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
\t<classpathentry kind="src" path="src"/>
\t<classpathentry kind="src" path="gen"/>
</classpath>
"""
        projectFile.exists()
        projectFile.text == """<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
\t<name>${project.name}</name>
\t<comment></comment>
\t<projects>
\t</projects>
\t<buildSpec>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ResourceManagerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.PreCompilerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ApkBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t</buildSpec>
\t<natures>
\t\t<nature>com.android.ide.eclipse.adt.AndroidNature</nature>
\t\t<nature>org.eclipse.jdt.core.javanature</nature>
\t</natures>
</projectDescription>
"""
        projectPropertiesFile.exists()
        projectPropertiesFile.text == """target=android-21
android.library.reference.1=externals/my.lib-1.0.0
android.library.reference.2=aarDependencies/com.android.support-appcompat-v7-21.0.2
android.library.reference.3=aarDependencies/com.melnykov-floatingactionbutton-1.0.7
android.library.reference.4=aarDependencies/com.github.ksoichiro-android-observablescrollview-1.5.0
android.library.reference.5=aarDependencies/com.android.support-support-v4-21.0.2
android.library.reference.6=aarDependencies/com.android.support-recyclerview-v7-21.0.0
"""
    }

    def "normalAndmoreProject"() {
        setup:
        Project project = ProjectBuilder
                .builder()
                .withProjectDir(temporaryFolder.root)
                .withName('normal')
                .build()
        project.plugins.apply AppPlugin
        project.plugins.apply PLUGIN_ID
        setupRepositories(project)
        project.dependencies {
            compile 'com.android.support:appcompat-v7:21.0.2'
            compile 'com.nineoldandroids:library:2.4.0'
            compile 'com.melnykov:floatingactionbutton:1.0.7'
            compile 'com.github.ksoichiro:android-observablescrollview:1.5.0'
        }
        temporaryFolder.newFolder('src')
        project.android.sourceSets.main.java.srcDirs = [ 'src' ]
        project.extensions.eclipseAar.andmore = true

        when:
        project.tasks.generateEclipseDependencies.execute()
        File classpathFile = project.file('.classpath')
        File projectFile = project.file('.project')
        File projectPropertiesFile = project.file('project.properties')

        then:
        classpathFile.exists()
        classpathFile.text == """<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="src" path="src"/>
\t<classpathentry kind="src" path="gen"/>
\t<classpathentry kind="con" path="org.eclipse.andmore.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="org.eclipse.andmore.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="org.eclipse.andmore.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
</classpath>
"""
        projectFile.exists()
        projectFile.text == """<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
\t<name>${project.name}</name>
\t<comment></comment>
\t<projects>
\t</projects>
\t<buildSpec>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.andmore.ResourceManagerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.andmore.PreCompilerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.andmore.ApkBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t</buildSpec>
\t<natures>
\t\t<nature>org.eclipse.andmore.AndroidNature</nature>
\t\t<nature>org.eclipse.jdt.core.javanature</nature>
\t</natures>
</projectDescription>
"""
        projectPropertiesFile.exists()
        projectPropertiesFile.text == """target=android-21
android.library.reference.1=aarDependencies/com.android.support-appcompat-v7-21.0.2
android.library.reference.2=aarDependencies/com.melnykov-floatingactionbutton-1.0.7
android.library.reference.3=aarDependencies/com.github.ksoichiro-android-observablescrollview-1.5.0
android.library.reference.4=aarDependencies/com.android.support-support-v4-21.0.2
android.library.reference.5=aarDependencies/com.android.support-recyclerview-v7-21.0.0
"""
    }

    def "Duplicate dependency names"() {
        setup:
        Project project = setupProject([
                'com.github.chrisbanes.photoview:library:1.2.3',
                'com.github.amlcurran.showcaseview:library:5.0.0',
        ])
        List<String> resolvedJars = [
                'com.android.support-support-v4-19.1.0.jar',
                'com.github.chrisbanes.photoview-library-1.2.3.jar',
                'com.github.amlcurran.showcaseview-library-5.0.0.jar',
        ]
        List<String> resolvedAars = [
                'com.github.chrisbanes.photoview-library-1.2.3',
                'com.github.amlcurran.showcaseview-library-5.0.0',
        ]

        when:
        project.tasks.generateEclipseDependencies.execute()
        List<String> jarNames = jarEntriesFromClasspathFiles(project)
        List<String> aarNames = aarEntriesFromProjectProperties(project)

        then:
        jarNames.find { !(it in resolvedJars) } == null
        aarNames.find { !(it in resolvedAars) } == null
    }

    def "Duplicate dependency artifacts"() {
        setup:
        Project project = setupProject([
                'com.github.chrisbanes.photoview:library:1.2.3',
                'com.github.amlcurran.showcaseview:library:5.0.0',
                'com.android.support:appcompat-v7:21.0.2',
        ])
        List<String> resolvedJars = [
                'com.android.support-appcompat-v7-21.0.2.jar',
                'com.android.support-support-v4-21.0.2.jar',
                'com.android.support-support-annotations-21.0.2.jar',
                'com.github.chrisbanes.photoview-library-1.2.3.jar',
                'com.github.amlcurran.showcaseview-library-5.0.0.jar',
        ]
        List<String> resolvedAars = [
                'com.android.support-appcompat-v7-21.0.2',
                'com.android.support-support-v4-21.0.2',
                'com.github.chrisbanes.photoview-library-1.2.3',
                'com.github.amlcurran.showcaseview-library-5.0.0',
        ]

        when:
        project.tasks.generateEclipseDependencies.execute()
        List<String> jarNames = jarEntriesFromClasspathFiles(project)
        List<String> aarNames = aarEntriesFromProjectProperties(project)

        then:
        jarNames.find { !(it in resolvedJars) } == null
        aarNames.find { !(it in resolvedAars) } == null
    }

    def "Raw jar file dependencies"() {
        setup:
        Project project = setupProject([
                'com.github.chrisbanes.photoview:library:1.2.3',
                'com.github.amlcurran.showcaseview:library:5.0.0',
                'com.android.support:appcompat-v7:21.0.2',
        ])
        List<String> resolvedJars = [
                'com.android.support-appcompat-v7-21.0.2.jar',
                'com.android.support-support-v4-21.0.2.jar',
                'com.android.support-support-annotations-21.0.2.jar',
                'com.github.chrisbanes.photoview-library-1.2.3.jar',
                'com.github.amlcurran.showcaseview-library-5.0.0.jar',
                'misc.jar'
        ]
        List<String> resolvedAars = [
                'com.android.support-appcompat-v7-21.0.2',
                'com.android.support-support-v4-21.0.2',
                'com.github.chrisbanes.photoview-library-1.2.3',
                'com.github.amlcurran.showcaseview-library-5.0.0',
        ]
        addStaticJarFileTo(temporaryFolder.newFolder('jarDependencies'))
        project.extensions.eclipseAar.jarDependenciesDir = 'jarDependencies'

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
        Project project = ProjectBuilder
                .builder()
                .withProjectDir(temporaryFolder.root)
                .build()
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
