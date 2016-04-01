package com.github.ksoichiro.eclipse.aar.generator

class ProjectPropertiesFileGenerator extends MetaDataFileGenerator {
    String androidTarget

    @Override
    String generateContent(File file) {
        """\
        |target=${androidTarget}
        |android.library=true
        |""".stripMargin()
    }
}
