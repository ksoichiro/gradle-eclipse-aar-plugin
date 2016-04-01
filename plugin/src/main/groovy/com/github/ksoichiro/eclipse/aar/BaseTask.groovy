package com.github.ksoichiro.eclipse.aar

import org.gradle.api.DefaultTask
import org.gradle.api.Project

class BaseTask extends DefaultTask {
    AarPluginExtension extension
    Set<AndroidProject> projects = []

    def findTargetProjects() {
        if (project.parent) {
            // Applied to sub project
            projects.addAll(project.parent.subprojects.collect { new AndroidProject(it) })
        } else {
            // Applied to root project
            projects << new AndroidProject(project)
            if (project.subprojects) {
                projects.addAll(project.subprojects.collect { new AndroidProject(it) })
            }
        }
        projects = projects.findAll { hasAndroidPlugin(it) }
    }

    static boolean hasAndroidPlugin(AndroidProject p) {
        p.project.plugins.hasPlugin('com.android.application') || p.project.plugins.hasPlugin('com.android.library')
    }
}
