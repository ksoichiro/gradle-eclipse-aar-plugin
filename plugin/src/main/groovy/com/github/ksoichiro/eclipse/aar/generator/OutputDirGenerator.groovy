package com.github.ksoichiro.eclipse.aar.generator

class OutputDirGenerator {
    void generate(File projectRootDir) {
        new File(projectRootDir, 'bin').mkdir()
        new File(projectRootDir, 'gen').mkdir()
    }
}
