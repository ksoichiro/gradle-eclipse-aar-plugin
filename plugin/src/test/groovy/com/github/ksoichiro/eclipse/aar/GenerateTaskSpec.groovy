package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.testfixtures.ProjectBuilder

class GenerateTaskSpec extends BaseSpec {
    def getQualifiedDependencyName() {
        setup:
        def dependency = Mock(ResolvedDependency)
        dependency.getModuleGroup() >> "moduleGroup"
        dependency.getModuleName() >> "moduleName"
        dependency.getModuleVersion() >> "1.0.0"

        expect:
        "moduleGroup:moduleName:1.0.0" == GenerateTask.getQualifiedDependencyName(dependency)
    }

    def getBaseName() {
        expect:
        result == GenerateTask.getBaseName(filename)

        where:
        filename                || result
        'artifact-version.ext'  || 'artifact-version'
        'support-v4-21.0.2.aar' || 'support-v4-21.0.2'
    }

    def versionIsNewerThan() {
        expect:
        result == GenerateTask.versionIsNewerThan(v1, v2)

        where:
        v1          | v2          || result
        '1.0.1'     | '1.0.0'     || true
        '1.0.0'     | '1.0.1'     || false
        '1.0.1'     | '1.0.1'     || false

        '2'         | '1'         || true
        '1'         | '2'         || false
        '2'         | '2'         || false

        '1.0.0'     | '1.0.0.rc1' || true
        '1.0.0.rc1' | '1.0.0'     || false
        '1.0.0.rc1' | '1.0.0.rc1' || false

        '1.10.0'    | '1.9.0'     || true
        '1.9.0'     | '1.10.0'    || false
        '1.10.0'    | '1.10.0'    || false
    }

    def "getDependencyFromFile found"() {
        setup:
        File dependencyFile = new File(
                "/this/path/does/not/exist/but/it/is/enough/for/this/test/"
                        + ".gradle/caches/modules-2/files-2.1/"
                        + "com.github.ksoichiro/android-observablescrollview/1.5.0/"
                        + "a44e58f48b8eac8c16cd99c35ed2f69078354999/"
                        + "android-observablescrollview-1.5.0.aar")
        def resolvedDependency = new DummyResolvedDependency(
                moduleGroup: "com.github.ksoichiro",
                moduleName: "android-observablescrollview",
                moduleVersion: "1.5.0")

        def resolvedDependency2 = new DummyResolvedDependency(
                moduleGroup: "com.nineoldandroids",
                moduleName: "library",
                moduleVersion: "2.4.0")

        def resolvedDependency3 = new DummyResolvedDependency(
                moduleGroup: "somebody",
                moduleName: "android-observablescrollview",
                moduleVersion: "1.5.0")

        Project project = ProjectBuilder.builder().build()
        project.plugins.apply PLUGIN_ID
        GenerateTask task = project.tasks.generateEclipseDependencies as GenerateTask
        task.allConfigurationsDependencies = [:]
        // This will be skipped
        task.allConfigurationsDependencies['com.nineoldandroids:library:2.4.0'] = resolvedDependency2
        // This will be also skipped
        task.allConfigurationsDependencies['somebody:android-observablescrollview:1.5.0'] = resolvedDependency3
        // And this will be assumed as the expected dependency
        task.allConfigurationsDependencies['com.github.ksoichiro:android-observablescrollview:1.5.0'] = resolvedDependency

        when:
        AndroidDependency dependency = task.getDependencyFromFile(dependencyFile)

        then:
        dependency
        AndroidArtifactType.AAR == dependency.artifactType
        'com.github.ksoichiro' == dependency.group
        'android-observablescrollview' == dependency.name
        '1.5.0' == dependency.version
        dependencyFile == dependency.file
    }

    def "getDependencyFromFile not found"() {
        setup:
        File dependencyFile = new File(
                "/this/path/does/not/exist/but/it/is/enough/for/this/test/"
                        + ".gradle/caches/modules-2/files-2.1/"
                        + "com.github.ksoichiro/android-observablescrollview/1.5.0/"
                        + "a44e58f48b8eac8c16cd99c35ed2f69078354999/"
                        + "android-observablescrollview-1.5.0.aar")

        Project project = ProjectBuilder.builder().build()
        project.plugins.apply PLUGIN_ID
        GenerateTask task = project.tasks.generateEclipseDependencies as GenerateTask
        task.allConfigurationsDependencies = [:]

        expect:
        null == task.getDependencyFromFile(dependencyFile)
    }
}
