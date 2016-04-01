package com.github.ksoichiro.eclipse.aar.generator

import org.gradle.api.Project

class ParentClasspathFileGenerator extends MetaDataFileGenerator {
    Project project

    @Override
    String generateContent(File file) {
        String result
        def classpathFile = file

        // Use srcDirs definition for classpath entry
        def androidSrcDirs = []
        project.android?.sourceSets?.findAll { it.name in ['main', 'debug'] }?.each {
            if (it.java?.srcDirs) {
                it.java.srcDirs.each { srcDir ->
                    if (srcDir.exists()) {
                        androidSrcDirs << srcDir
                    }
                }
            }
        }
        if (0 == androidSrcDirs.size()) {
            androidSrcDirs = ['src/main/java']
        }
        if (!androidSrcDirs.contains('gen')) {
            androidSrcDirs << 'gen'
        }
        def androidSrcPaths = []
        androidSrcDirs.each {
            androidSrcPaths << (it.toString() - project.projectDir.path).replaceFirst("^[/\\\\]", '')
        }
        List<String> srcPaths = []
        if (classpathFile.exists()) {
            // Aggregate src paths and dependencies
            def classPaths = new XmlSlurper().parseText(classpathFile.text)
            def srcPathEntries = classPaths.classpathentry?.findAll { it.@kind?.text() == 'src' }
            srcPaths = srcPathEntries.collect { it.@path.text() }

            result = classpathFile.text
        } else {
            // Create minimum classpath file
            srcPaths = androidSrcPaths
            def srcPathEntries = androidSrcPaths.collect {
                """
                |\t<classpathentry kind="src" path="${it}"/>""".stripMargin()
            }.join('')
            result = """\
                |<?xml version="1.0" encoding="UTF-8"?>
                |<classpath>${srcPathEntries}
                |\t<classpathentry kind="con" path="${toolPackage}.ANDROID_FRAMEWORK"/>
                |\t<classpathentry exported="true" kind="con" path="${toolPackage}.LIBRARIES"/>
                |\t<classpathentry exported="true" kind="con" path="${toolPackage}.DEPENDENCIES"/>
                |\t<classpathentry kind="output" path="bin/classes"/>
                |</classpath>
                |""".stripMargin()
        }

        androidSrcPaths = androidSrcPaths.findAll { srcPaths.find { path -> path == it } == null }
        if (androidSrcPaths) {
            def entriesToAdd = androidSrcPaths.collect { it -> "\t<classpathentry kind=\"src\" path=\"${it}\"/>" }
            def lines = result.readLines()?.findAll { it != '</classpath>' }
            lines += entriesToAdd
            lines += "</classpath>${System.getProperty('line.separator')}"
            result = lines.join(System.getProperty('line.separator'))
        }
        result
    }
}
