package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project

class AarPluginExtension {
    Project project
    String androidTarget = 'android-21'
    String aarDependenciesDir = 'aarDependencies'
    String jarDependenciesDir
    String projectNamePrefix = ''
    boolean cleanLibsDirectoryEnabled = false
    String projectName
    List<String> targetConfigurations = ['compile', 'debugCompile']
    final String commit
    final String committedAt
    final String builtAt

    AarPluginExtension(Project project) {
        this.project = project
        def versionInfo = new ConfigSlurper().parse(getClass().getClassLoader().getResourceAsStream('version.groovy').text)
        this.commit = versionInfo.version.commit
        this.committedAt = versionInfo.version.committedAt
        this.builtAt = versionInfo.version.builtAt
    }
}
