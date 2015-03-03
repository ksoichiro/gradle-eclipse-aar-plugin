package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Plugin
import org.gradle.api.Project

class AarPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('eclipseAar', AarPluginExtension, project)
        def cleanEclipseDependencies = project.task('cleanEclipseDependencies', type: CleanTask)
        project.task('generateEclipseDependencies', type: GenerateTask, dependsOn: cleanEclipseDependencies)
    }
}
