package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class GenerateTask extends BaseTask {
    Set<File> jarDependencies
    Set<File> aarDependencies

    static {
        String.metaClass.isNewerThan = { String v2 ->
            String v1 = delegate
            def versions1 = v1.tokenize('.')
            def versions2 = v2.tokenize('.')
            for (int i = 0; i < Math.min(versions1.size(), versions2.size()); i++) {
                int n1 = versions1[i].toInteger()
                int n2 = versions2[i].toInteger()
                if (n2 < n1) {
                    return true
                }
            }
            versions2.size() < versions1.size()
        }
    }

    GenerateTask() {
        description = 'Used for Eclipse. Copies all AAR dependencies for library directory.'
    }

    @TaskAction
    def exec() {
        extension = project.eclipseAar
        jarDependencies = [] as Set<File>
        aarDependencies = [] as Set<File>

        findTargetProjects()

        def aggregateDependenciesFrom = { Project p ->
            androidConfigurations().each {
                println "Aggregating JAR dependencies from ${it.name} configuration"
                it.filter {
                    it.name.endsWith 'jar'
                }.each { File jar -> jarDependencies << jar }

                println "Aggregating AAR dependencies from ${it.name} configuration"
                it.filter { File aar ->
                    aar.name.endsWith('aar') && isRequired(p, aar)
                }.each { File aar -> aarDependencies << aar }
            }
        }

        projects.each {
            aggregateDependenciesFrom it
        }

        def extractDependenciesFrom = { Project p ->
            androidConfigurations().each {
                println "Extracting JAR dependencies from ${it.name} configuration"
                it.filter {
                    it.name.endsWith 'jar'
                }.each { File jar -> moveJarIntoLibs(p, jar) }

                println "Extracting AAR dependencies from ${it.name} configuration"
                it.filter { File aar ->
                    aar.name.endsWith('aar') && isRequired(p, aar)
                }.each { File aar -> moveAndRenameAar(p, aar) }
            }
        }

        projects.each {
            extractDependenciesFrom it
        }

        projects.each { Project p ->
            List<File> aars = p?.file(extension.aarDependenciesDir)?.listFiles()?.findAll {
                it.isDirectory()
            }
            aars?.each { aar ->
                generateProjectPropertiesFile(p, aar)
                generateEclipseClasspathFile(p, aar)
                generateEclipseProjectFile(p, aar)
            }
            generateEclipseClasspathFileForParent(p)
        }
    }

    def androidConfigurations() {
        def result = []
        projects.each {
            result.addAll([it.configurations.compile, it.configurations.debugCompile])
        }
        result
    }

    static String getAarJarFilename(File file) {
        "${getBaseName(file.name)}.jar"
    }

    static String getDependencyProjectName(File file) {
        file.name.lastIndexOf('.').with { it != -1 ? file.name[0..<it] : file.name }
    }

    static String getBaseName(String filename) {
        filename.lastIndexOf('.').with { it != -1 ? filename[0..<it] : filename }
    }

    static String getDependencyName(String jarFilename) {
        def baseFilename = getBaseName(jarFilename)
        baseFilename.lastIndexOf('-').with { it != -1 ? baseFilename[0..<it] : baseFilename }
    }

    static String getVersionName(String jarFilename) {
        def baseFilename = getBaseName(jarFilename)
        baseFilename.lastIndexOf('-').with { it != -1 ? baseFilename.substring(it + 1) : baseFilename }
    }

    boolean isRequired(Project p, File aar) {
        def name = getDependencyProjectName(aar)
        boolean result = false
        p.file('project.properties').eachLine { String line ->
            if (line.matches("^android.library.reference.[0-9]+=${extension.aarDependenciesDir}/${name}")) {
                result = true
            }
        }
        result
    }

    void moveJarIntoLibs(Project p, File file) {
        println "Added jar ${file}"
        copyJarIfNewer(p, 'libs', file, false)
    }

    void moveAndRenameAar(Project p, File file) {
        println "Added aar ${file}"
        def dependencyProjectName = getDependencyProjectName(file)

        // directory excluding the classes.jar
        p.copy {
            from p.zipTree(file)
            exclude 'classes.jar'
            into "${extension.aarDependenciesDir}/${dependencyProjectName}"
        }

        // Copies the classes.jar into the libs directory of the exploded AAR.
        // In Eclipse you can then import this exploded ar as an Android project
        // and then reference not only the classes but also the android resources :D
        ["${extension.aarDependenciesDir}/${dependencyProjectName}/libs", "libs"].each { dest ->
            copyJarIfNewer(p, dest, file, true)
        }
    }

    void copyJarIfNewer(Project p, String libsDir, File dependency, boolean isAarDependency) {
        def dependencyFilename = dependency.name
        def dependencyProjectName = getDependencyProjectName(dependency)
        def dependencyName = getDependencyName(dependencyFilename)
        def versionName = getVersionName(dependencyFilename)
        boolean isNewer = false
        boolean sameDependencyExists = false
        def dependencies = isAarDependency ? aarDependencies : jarDependencies
        def copyClosure = isAarDependency ? { destDir ->
            p.copy {
                from p.zipTree(dependency)
                exclude 'classes.jar'
                into "${extension.aarDependenciesDir}/${dependencyProjectName}"
            }
            p.copy {
                from p.zipTree(dependency)
                include 'classes.jar'
                into destDir
                rename { String fileName ->
                    fileName.replace('classes.jar', "${dependencyProjectName}.jar")
                }
            }
        } : { destDir ->
            p.copy {
                from dependency
                into destDir
            }
        }
        dependencies.findAll { File it ->
            // Check if there are any dependencies with the same name but different version
            getDependencyName(it.name) == dependencyName && getVersionName(it.name) != versionName
        }.each { File file ->
            println "  Same dependency exists: ${dependencyFilename}, ${file.name}"
            sameDependencyExists = true
            def v1 = getVersionName(dependencyFilename)
            def v2 = getVersionName(file.name)
            // 'file' may be removed in previous loop
            if (file.exists() && v1.isNewerThan(v2)) {
                println "  Found older dependency. Copy ${dependencyFilename} to all subprojects"
                isNewer = true
                // Should be replaced to jarFilename jar
                projects.each { Project pp ->
                    def projectLibDir = pp.file('libs')
                    if (isAarDependency) {
                        projectLibDir.listFiles().findAll {
                            it.isDirectory() && getDependencyName(it.name) == dependencyName
                        }.each { File lib ->
                            println "  REMOVED ${lib}"
                            pp.delete(lib)
                            pp.copy {
                                from pp.zipTree(dependency)
                                exclude 'classes.jar'
                                into "${extension.aarDependenciesDir}/${dependencyProjectName}"
                            }
                            copyClosure(projectLibDir)
                        }
                    } else {
                        projectLibDir.listFiles().findAll {
                            !it.isDirectory() && getDependencyName(it.name) == dependencyName
                        }.each { File lib ->
                            println "  REMOVED ${lib}"
                            pp.delete(lib)
                            copyClosure(projectLibDir)
                        }
                    }
                }
            }
        }
        if (!sameDependencyExists || isNewer) {
            println "  Copy new dependency: ${dependencyFilename}"
            copyClosure(libsDir)
        }
    }

    void generateProjectPropertiesFile(Project p, File aar) {
        p.file("${extension.aarDependenciesDir}/${aar.name}/project.properties").text = """\
target=${extension.androidTarget}
android.library=true
"""
    }

    void generateEclipseClasspathFile(Project p, File aar) {
        p.file("${extension.aarDependenciesDir}/${aar.name}/.classpath").text = """\
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" path="gen"/>
	<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
	<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
	<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
	<classpathentry exported="true" kind="con" path="org.springsource.ide.eclipse.gradle.classpathcontainer"/>
	<classpathentry kind="output" path="bin/classes"/>
</classpath>
"""
    }

    void generateEclipseProjectFile(Project p, File aar) {
        def projectName = extension.projectName ?: p.name
        def name = aar.name
        p.file("${extension.aarDependenciesDir}/${name}/.project").text = """\
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>${extension.projectNamePrefix}${projectName}-${name}</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>com.android.ide.eclipse.adt.ResourceManagerBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.android.ide.eclipse.adt.PreCompilerBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.android.ide.eclipse.adt.ApkBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.springsource.ide.eclipse.gradle.core.nature</nature>
		<nature>org.eclipse.jdt.core.javanature</nature>
		<nature>com.android.ide.eclipse.adt.AndroidNature</nature>
	</natures>
</projectDescription>
"""
    }

    void generateEclipseClasspathFileForParent(Project p) {
        def classpathFile = p.file('.classpath')
        def libNames = []
        if (classpathFile.exists()) {
            // Aggregate dependencies
            def classPaths = new XmlSlurper().parseText(classpathFile.text)
            def libClassPathEntries = classPaths.classpathentry?.findAll { it.@kind?.text() == 'lib' }
            libNames = libClassPathEntries.collect { it.@path.text().replaceFirst('^libs/', '') }
        } else {
            // Create minimum classpath file
            classpathFile.text = """\
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
\t<classpathentry kind="src" path="src"/>
\t<classpathentry kind="src" path="gen"/>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
</classpath>
"""
        }
        def additionalClassPathEntries = []
        def jars = []
        jars.addAll(jarDependencies.collect { it.name })
        jars.addAll(aarDependencies.collect { getAarJarFilename(it) })
        jars = jars.findAll { !(it in libNames) }
        jars.each { jar ->
            additionalClassPathEntries << "<classpathentry kind=\"lib\" path=\"libs/${jar}\"/>"
        }
        if (0 < additionalClassPathEntries.size()) {
            def lines = []
            classpathFile.eachLine { line ->
                if (line != '</classpath>') {
                    lines << line
                }
            }
            additionalClassPathEntries.each { classPathEntry ->
                lines << "\t${classPathEntry}"
            }
            lines << "</classpath>${System.getProperty('line.separator')}"
            classpathFile.text = lines.join(System.getProperty('line.separator'))
        }
    }
}
