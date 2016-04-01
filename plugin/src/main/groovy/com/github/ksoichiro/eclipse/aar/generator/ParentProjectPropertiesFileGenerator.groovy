package com.github.ksoichiro.eclipse.aar.generator

import com.github.ksoichiro.eclipse.aar.AndroidArtifactType
import com.github.ksoichiro.eclipse.aar.AndroidDependency
import org.gradle.api.Project

import java.util.regex.Matcher

class ParentProjectPropertiesFileGenerator extends MetaDataFileGenerator {
    Project project
    String androidTarget
    String aarDependenciesDir
    Set<AndroidDependency> fileDependencies
    Set<AndroidDependency> projectDependencies

    @Override
    String generateContent(File file) {
        String result
        def projectPropertiesFile = file
        List<String> libNames = []
        List<String> projectNames = []
        int maxReference = 0
        boolean shouldAddLibrary = false
        if (project.plugins.hasPlugin('com.android.library')) {
            shouldAddLibrary = true
        }
        if (projectPropertiesFile.exists()) {
            Properties props = new Properties()
            projectPropertiesFile.withInputStream { stream -> props.load(stream) }

            props.propertyNames().findAll {
                it =~ /^android\.library\.reference\.[0-9]+/
            }.each {
                Matcher mValue = props[it] =~ /^${aarDependenciesDir}\\/(.*)/
                if (mValue.matches()) {
                    libNames << mValue[0][1]
                    Matcher mName = it =~ /^android\.library\.reference\.([0-9]+)/
                    if (mName.matches()) {
                        int ref = mName[0][1].toInteger()
                        if (maxReference < ref) {
                            maxReference = ref
                        }
                    }
                } else {
                    mValue = props[it] =~ /^\.\.\/(.*)/
                    if (mValue.matches()) {
                        projectNames << mValue[0][1]
                        Matcher mName = it =~ /^android\.library\.reference\.([0-9]+)/
                        if (mName.matches()) {
                            int ref = mName[0][1].toInteger()
                            if (maxReference < ref) {
                                maxReference = ref
                            }
                        }
                    }
                }
            }
            if (shouldAddLibrary && props.containsKey('android.library')) {
                shouldAddLibrary = false
            }

            result = projectPropertiesFile.text
        } else {
            // Create minimum properties file
            result = """\
                |target=${androidTarget}
                |""".stripMargin()
        }

        def entriesToAdd = []
        if (shouldAddLibrary) {
            entriesToAdd << 'android.library=true'
        }

        List<String> list = projectDependencies?.collect { it.getQualifiedName().replaceFirst('^:', '') }
        list = list?.findAll { projectNames.find { prj -> prj == it } == null }
        list?.unique()
        list?.each {
            maxReference++
            entriesToAdd << "android.library.reference.${maxReference}=../${it}"
        }

        List<String> aars = fileDependencies.findAll { it.artifactType == AndroidArtifactType.AAR }?.collect { it.getQualifiedName() }
        aars = aars?.findAll { libNames.find { lib -> lib == it } == null }
        aars?.unique()
        aars?.each {
            maxReference++
            entriesToAdd << "android.library.reference.${maxReference}=${aarDependenciesDir}/${it}"
        }

        if (0 < entriesToAdd.size()) {
            def content = result
            if (!content.endsWith(System.getProperty('line.separator'))) {
                content += System.getProperty('line.separator')
            }
            result = content + entriesToAdd.join(System.getProperty('line.separator')) + System.getProperty('line.separator')
        }
        result
    }
}
