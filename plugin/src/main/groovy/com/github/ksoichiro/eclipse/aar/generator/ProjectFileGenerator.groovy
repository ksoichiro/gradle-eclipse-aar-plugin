package com.github.ksoichiro.eclipse.aar.generator

class ProjectFileGenerator extends MetaDataFileGenerator {
    String projectName

    @Override
    boolean shouldOverwrite() {
        false
    }

    @Override
    String generateContent(File file) {
        // Note about natures: AndroidNature should be first to be recognized
        // as an Android project in Eclipse.
        """\
        |<?xml version="1.0" encoding="UTF-8"?>
        |<projectDescription>
        |\t<name>${projectName}</name>
        |\t<comment></comment>
        |\t<projects>
        |\t</projects>
        |\t<buildSpec>
        |\t\t<buildCommand>
        |\t\t\t<name>${toolPackage}.ResourceManagerBuilder</name>
        |\t\t\t<arguments>
        |\t\t\t</arguments>
        |\t\t</buildCommand>
        |\t\t<buildCommand>
        |\t\t\t<name>${toolPackage}.PreCompilerBuilder</name>
        |\t\t\t<arguments>
        |\t\t\t</arguments>
        |\t\t</buildCommand>
        |\t\t<buildCommand>
        |\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>
        |\t\t\t<arguments>
        |\t\t\t</arguments>
        |\t\t</buildCommand>
        |\t\t<buildCommand>
        |\t\t\t<name>${toolPackage}.ApkBuilder</name>
        |\t\t\t<arguments>
        |\t\t\t</arguments>
        |\t\t</buildCommand>
        |\t</buildSpec>
        |\t<natures>
        |\t\t<nature>${toolPackage}.AndroidNature</nature>
        |\t\t<nature>org.eclipse.jdt.core.javanature</nature>
        |\t</natures>
        |</projectDescription>
        |""".stripMargin()
    }
}
