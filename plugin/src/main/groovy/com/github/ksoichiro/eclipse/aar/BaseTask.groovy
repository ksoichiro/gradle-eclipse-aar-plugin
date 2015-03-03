package com.github.ksoichiro.eclipse.aar

import org.gradle.api.DefaultTask
import org.gradle.api.Project

class BaseTask extends DefaultTask {
    AarPluginExtension extension
    Set<AndroidProject> projects = []

    def findTargetProjects() {
        if (project.parent) {
            // Applied to sub project
            project.parent.subprojects.each { Project p ->
                projects << new AndroidProject(p)
            }
        } else {
            // Applied to root project
            projects << new AndroidProject(project)
            if (project.subprojects) {
                project.subprojects.each { Project p ->
                    projects << new AndroidProject(p)
                }
            }
        }
        projects = projects.findAll { hasAndroidPlugin(it) }
    }

    static boolean hasAndroidPlugin(AndroidProject p) {
        p.project.plugins.hasPlugin('com.android.application') || p.project.plugins.hasPlugin('com.android.library')
    }
}
