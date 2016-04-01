package com.github.ksoichiro.eclipse.aar.generator

abstract class MetaDataFileGenerator {
    static final ADT_PACKAGE = 'com.android.ide.eclipse.adt'
    static final ANDMORE_PACKAGE = 'org.eclipse.andmore'
    boolean andmore
    String toolPackage

    abstract String generateContent(File file)

    boolean shouldOverwrite() {
        true
    }

    void generate(File file) {
        if (file.canonicalFile.exists() && !shouldOverwrite()) {
            return
        }
        toolPackage = andmore ? ANDMORE_PACKAGE : ADT_PACKAGE
        file.text = generateContent(file)
    }
}
