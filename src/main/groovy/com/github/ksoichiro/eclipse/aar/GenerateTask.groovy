package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.TaskAction

class GenerateTask extends BaseTask {
    Map<Project, Set<AndroidDependency>> jarDependencies
    Map<Project, Set<AndroidDependency>> aarDependencies

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
        jarDependencies = [:]
        aarDependencies = [:]

        findTargetProjects()

        def allConfigurationsDependencies = [:] as Map<String, ResolvedDependency>
        def aggregateResolvedDependencies
        aggregateResolvedDependencies = { Set<ResolvedDependency> it, String indent ->
            it.each { ResolvedDependency dependency ->
                if (!allConfigurationsDependencies.containsKey(getQualifiedDependencyName(dependency))) {
                    println "${indent}${getQualifiedDependencyName(dependency)}"
                    allConfigurationsDependencies[getQualifiedDependencyName(dependency)] = dependency
                }
                if (dependency.children) {
                    aggregateResolvedDependencies(dependency.children, indent + "  ")
                }
            }
        }
        projects.each { Project p ->
            androidConfigurations(p).each { Configuration configuration ->
                println "Aggregating resolved dependencies for project ${p.name} from ${configuration.name} configuration"
                aggregateResolvedDependencies(configuration.resolvedConfiguration.firstLevelModuleDependencies, "  ")

                println "Aggregating JAR dependencies for project ${p.name} from ${configuration.name} configuration"
                configuration.filter {
                    it.name.endsWith 'jar'
                }.each { File jar ->
                    def d = new AndroidDependency()
                    d.with {
                        file = jar
                        artifactType = AndroidArtifactType.JAR
                    }
                    if (!jarDependencies[p]) {
                        jarDependencies[p] = [] as Set<AndroidDependency>
                    }
                    jarDependencies[p] << d
                }

                println "Aggregating AAR dependencies for project ${p.name} from ${configuration.name} configuration"
                configuration.filter { File aar ->
                    aar.name.endsWith('aar')
                }.each { File aar ->
                    def convertedPath = aar.path.tr(System.getProperty('file.separator'), '.')
                    def convertedPathExtStripped = convertedPath.lastIndexOf('.').with {
                        it != -1 ? convertedPath[0..<it] : convertedPath
                    }
                    def localDependency = configuration.dependencies.find { d -> convertedPathExtStripped.endsWith("${d.name}-release") }
                    if (localDependency instanceof ProjectDependency) {
                        // ProjectDependency should be not be exploded, just include in project.properties with relative path
                        println "  Skip ProjectDependency: ${localDependency} for file ${aar}"
                    } else {
                        def d = new AndroidDependency()
                        d.with {
                            file = aar
                            artifactType = AndroidArtifactType.AAR
                        }
                        if (!aarDependencies[p]) {
                            aarDependencies[p] = [] as Set<AndroidDependency>
                        }
                        aarDependencies[p] << d
                    }
                }
            }
        }
        projects.each { Project p ->
            jarDependencies[p] = getLatestDependencies(jarDependencies, jarDependencies[p])
            aarDependencies[p] = getLatestDependencies(aarDependencies, aarDependencies[p])

            (jarDependencies[p] + aarDependencies[p]).each { AndroidDependency d ->
                String convertedPath = d.file.path.tr(System.getProperty('file.separator'), '.')
                ResolvedDependency matchedDependency = allConfigurationsDependencies.find { k, v ->
                    convertedPath.contains("${v.moduleGroup}.${v.moduleName}") && v.moduleVersion == d.version
                }?.value
                if (matchedDependency) {
                    d.with {
                        group = matchedDependency.moduleGroup
                        name = matchedDependency.moduleName
                        version = matchedDependency.moduleVersion
                    }
                } else {
                    println "WARNING: matching dependency not found for ${d.file}"
                }
            }
        }

        def extractDependenciesFrom = { Project p ->
            jarDependencies[p].each { AndroidDependency d -> moveJarIntoLibs(p, d) }
            aarDependencies[p].each { AndroidDependency d -> moveAndRenameAar(p, d) }
        }

        projects.each {
            extractDependenciesFrom it
        }

        projects.each { Project p ->
            aarDependencies[p].each { AndroidDependency d ->
                generateProjectPropertiesFile(p, d)
                generateEclipseClasspathFile(p, d)
                generateEclipseProjectFile(p, d)
            }
            generateEclipseClasspathFileForParent(p)
        }
    }

    static String getQualifiedDependencyName(ResolvedDependency dependency) {
        "${dependency.moduleGroup}:${dependency.moduleName}:${dependency.moduleVersion}"
    }

    static Collection<Configuration> androidConfigurations(Project p) {
        [p.configurations.compile, p.configurations.debugCompile]
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

    static Set<AndroidDependency> getLatestDependencies(Map<Project, Set<AndroidDependency>> dependencies, Set<AndroidDependency> projectDependencies) {
        Set<AndroidDependency> allDependencies = []
        for (Set<AndroidDependency> d : dependencies.values()) {
            d.each { AndroidDependency dependency ->
                allDependencies << dependency
            }
        }

        Set<AndroidDependency> latestDependencies = []
        projectDependencies.each { AndroidDependency dependency ->
            def dependencyName = getDependencyName(dependency.file.name)
            String latestJarVersion = "0"
            def duplicateDependencies = allDependencies.findAll { it.file.name.startsWith(dependencyName) }
            AndroidDependency latestDependency
            if (1 < duplicateDependencies.size()) {
                duplicateDependencies.each {
                    if (getVersionName(it.file.name).isNewerThan(latestJarVersion)) {
                        latestJarVersion = getVersionName(it.file.name)
                    }
                }
                latestDependency = duplicateDependencies.find { getVersionName(it.file.name) == latestJarVersion }
            } else {
                latestJarVersion = getVersionName(dependency.file.name)
                latestDependency = dependency
            }
            if (latestDependency) {
                latestDependency.version = latestJarVersion
                latestDependencies << latestDependency
            }
        }
        latestDependencies
    }

    void moveJarIntoLibs(Project p, AndroidDependency dependency) {
        println "Added jar ${dependency.file}"
        copyJarIfNewer(p, 'libs', dependency, false)
    }

    void moveAndRenameAar(Project p, AndroidDependency dependency) {
        println "Added aar ${dependency.file}"
        def dependencyProjectName = dependency.getQualifiedName()

        p.copy {
            from p.zipTree(dependency.file)
            exclude 'classes.jar'
            into "${extension.aarDependenciesDir}/${dependencyProjectName}"
        }

        ["${extension.aarDependenciesDir}/${dependencyProjectName}/libs", "libs"].each { dest ->
            copyJarIfNewer(p, dest, dependency, true)
        }
    }

    void copyJarIfNewer(Project p, String libsDir, AndroidDependency dependency, boolean isAarDependency) {
        def dependencyFilename = dependency.file.name
        def dependencyProjectName = dependency.getQualifiedName()
        def dependencyName = getDependencyName(dependencyFilename)
        def versionName = getVersionName(dependencyFilename)
        boolean isNewer = false
        boolean sameDependencyExists = false
        def dependencies = isAarDependency ? aarDependencies[p] : jarDependencies[p]
        def copyClosure = isAarDependency ? { destDir ->
            p.copy {
                from p.zipTree(dependency.file)
                exclude 'classes.jar'
                into "${extension.aarDependenciesDir}/${dependencyProjectName}"
            }
            p.copy {
                from p.zipTree(dependency.file)
                include 'classes.jar'
                into destDir
                rename { String fileName ->
                    fileName.replace('classes.jar', "${dependencyProjectName}.jar")
                }
            }
        } : { destDir ->
            p.copy {
                from dependency.file
                into destDir
                rename { "${dependencyProjectName}.jar" }
            }
        }
        dependencies.findAll { AndroidDependency it ->
            // Check if there are any dependencies with the same name but different version
            getDependencyName(it.file.name) == dependencyName && getVersionName(it.file.name) != versionName
        }.each { AndroidDependency androidDependency ->
            println "  Same dependency exists: ${dependencyFilename}, ${androidDependency.file.name}"
            sameDependencyExists = true
            def v1 = getVersionName(dependencyFilename)
            def v2 = getVersionName(androidDependency.file.name)
            // 'androidDependency.file' may be removed in previous loop
            if (androidDependency.file.exists() && v1.isNewerThan(v2)) {
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
                                from pp.zipTree(dependency.file)
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

    void generateProjectPropertiesFile(Project p, AndroidDependency dependency) {
        p.file("${extension.aarDependenciesDir}/${dependency.getQualifiedName()}/project.properties").text = """\
target=${extension.androidTarget}
android.library=true
"""
    }

    void generateEclipseClasspathFile(Project p, AndroidDependency dependency) {
        p.file("${extension.aarDependenciesDir}/${dependency.getQualifiedName()}/.classpath").text = """\
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" path="gen"/>
	<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
	<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
	<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
	<classpathentry kind="output" path="bin/classes"/>
</classpath>
"""
    }

    void generateEclipseProjectFile(Project p, AndroidDependency dependency) {
        def projectName = extension.projectName ?: p.name
        def name = dependency.getQualifiedName()
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
        List<String> libNames = []
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
        List<String> jars = jarDependencies[p].collect { "${it.getQualifiedName()}.jar" } + aarDependencies[p].collect { "${it.getQualifiedName()}.jar" }
        jars = jars.findAll { libNames.find { lib -> lib == it} == null }
        if (jars) {
            def entriesToAdd = jars.collect { it -> "\t<classpathentry kind=\"lib\" path=\"libs/${it}\"/>" }
            def lines = classpathFile.readLines()?.findAll { it != '</classpath>' }
            lines += entriesToAdd
            lines += "</classpath>${System.getProperty('line.separator')}"
            classpathFile.text = lines.join(System.getProperty('line.separator'))
        }
    }
}
