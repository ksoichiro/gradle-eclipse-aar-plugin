package com.github.ksoichiro.eclipse.aar.generator

class ClasspathFileGenerator extends MetaDataFileGenerator {
    @Override
    String generateContent(File file) {
        """\
        |<?xml version="1.0" encoding="UTF-8"?>
        |<classpath>
        |\t<classpathentry kind="src" path="gen"/>
        |\t<classpathentry kind="con" path="${toolPackage}.ANDROID_FRAMEWORK"/>
        |\t<classpathentry exported="true" kind="con" path="${toolPackage}.LIBRARIES"/>
        |\t<classpathentry exported="true" kind="con" path="${toolPackage}.DEPENDENCIES"/>
        |\t<classpathentry kind="output" path="bin/classes"/>
        |</classpath>
        |""".stripMargin()
    }
}
