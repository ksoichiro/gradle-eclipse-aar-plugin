package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project
import spock.lang.Specification

class BaseSpec extends Specification {
    static final String PLUGIN_ID = 'com.github.ksoichiro.eclipse.aar'

    void deleteOutputs(Project project) {
        ['.gradle', 'userHome', 'aarDependencies', 'libs', '.classpath', '.project', 'project.properties'].each {
            if (project.file(it).exists()) {
                project.delete(it)
            }
        }
    }
}
