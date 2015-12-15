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

    AarPluginExtension(Project project) {
        this.project = project
    }
}
