package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CleanTask extends BaseTask {
    CleanTask() {
        description = 'Used for Eclipse. Cleans AAR dependencies directory.'
    }

    @TaskAction
    def exec() {
        extension = project.eclipseAar

        findTargetProjects()
        projects.each { Project p ->
            [ p.file(extension.aarDependenciesDir), p.file('libs') ].each {
                if (it.exists()) {
                    p.delete(it)
                    println "Deleted ${it}"
                }
            }
        }
    }
}
