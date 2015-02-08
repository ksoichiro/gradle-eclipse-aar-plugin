package com.github.ksoichiro.eclipse.aar

import org.gradle.api.DefaultTask
import org.gradle.api.Project

class BaseTask extends DefaultTask {
    AarPluginExtension extension
    Set<Project> projects = []

    def findTargetProjects() {
        if (project.parent) {
            // Applied to sub project
            projects.addAll(project.parent.subprojects)
        } else {
            // Applied to root project
            projects << project
            if (project.subprojects) {
                projects.addAll(project.subprojects)
            }
        }
        projects = projects.findAll { hasAndroidPlugin(it) }
    }

    static boolean hasAndroidPlugin(Project p) {
        p.plugins.hasPlugin('com.android.application') || p.plugins.hasPlugin('com.android.library')
    }
}
