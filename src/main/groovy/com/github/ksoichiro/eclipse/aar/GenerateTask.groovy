package com.github.ksoichiro.eclipse.aar

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.TaskAction
import org.gradle.mvn3.org.apache.maven.artifact.versioning.ComparableVersion

import java.util.regex.Matcher

class GenerateTask extends BaseTask {
    Map<Project, Set<AndroidDependency>> jarDependencies
    Map<Project, Set<AndroidDependency>> aarDependencies
    Map<Project, Set<AndroidDependency>> projectDependencies
    Map<String, ResolvedDependency> allConfigurationsDependencies

    GenerateTask() {
        description = 'Used for Eclipse. Copies all AAR dependencies for library directory.'
    }

    @TaskAction
    def exec() {
        extension = project.eclipseAar
        jarDependencies = [:]
        aarDependencies = [:]
        projectDependencies = [:]

        findTargetProjects()

        allConfigurationsDependencies = [:]
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
                        Project dependencyProject = projects.find { it.name == ((ProjectDependency) localDependency).dependencyProject.name }
                        def d = new AndroidDependency()
                        d.with {
                            name = dependencyProject.name
                            artifactType = AndroidArtifactType.PROJECT
                        }
                        if (!projectDependencies[p]) {
                            projectDependencies[p] = [] as Set<AndroidDependency>
                        }
                        projectDependencies[p] << d
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
            generateEclipseProjectFileForParent(p)
            generateProjectPropertiesFileForParent(p)
        }
    }

    static String getQualifiedDependencyName(ResolvedDependency dependency) {
        "${dependency.moduleGroup}:${dependency.moduleName}:${dependency.moduleVersion}"
    }

    Collection<Configuration> androidConfigurations(Project p) {
        extension.targetConfigurations.collect { p.configurations.getByName(it) }
    }

    static String getBaseName(String filename) {
        filename.lastIndexOf('.').with { it != -1 ? filename[0..<it] : filename }
    }

    String getDependencyGroup(File file) {
        // '-' is not always the separator of version, version itself often has '-'.
        // So we should find version from resolved dependencies.
        // Path: .../modules-2/files-2.1/group/artifact/version/hash/artifact-version.ext
        def baseFilename = getBaseName(file.name)
        String target = file.path.tr(System.getProperty('file.separator'), '-')
        String group = null
        allConfigurationsDependencies.each { k, v ->
            if ("${v.moduleName}-${v.moduleVersion}" == baseFilename) {
                // This may be the dependency of the file
                // Find group from file path and check qualified name matches
                if (target ==~ /.*-${v.moduleGroup}-${v.moduleName}-${v.moduleVersion}-.*/) {
                    // This is the one
                    group = v.moduleGroup
                }
            }
        }
        if (!group) {
            println "ERROR: Could not find group: ${target}"
        }
        return group
    }

    String getDependencyName(File file) {
        // '-' is not always the separator of version, version itself often has '-'.
        // So we should find version from resolved dependencies.
        // Path: .../modules-2/files-2.1/group/artifact/version/hash/artifact-version.ext
        def baseFilename = getBaseName(file.name)
        String target = file.path.tr(System.getProperty('file.separator'), '-')
        String name = null
        allConfigurationsDependencies.each { k, v ->
            if ("${v.moduleName}-${v.moduleVersion}" == baseFilename) {
                // This may be the dependency of the file
                // Find group from file path and check qualified name matches
                if (target ==~ /.*-${v.moduleGroup}-${v.moduleName}-${v.moduleVersion}-.*/) {
                    // This is the one
                    name = v.moduleName
                }
            }
        }
        if (!name) {
            println "ERROR: Could not find name: ${target}"
        }
        return name
    }

    String getVersionName(File file) {
        // '-' is not always the separator of version, version itself often has '-'.
        // So we should find version from resolved dependencies.
        // Path: .../modules-2/files-2.1/group/artifact/version/hash/artifact-version.ext
        def baseFilename = getBaseName(file.name)
        String target = file.path.tr(System.getProperty('file.separator'), '-')
        String version = null
        allConfigurationsDependencies.each { k, v ->
            if ("${v.moduleName}-${v.moduleVersion}" == baseFilename) {
                // This may be the dependency of the file
                // Find group from file path and check qualified name matches
                if (target ==~ /.*-${v.moduleGroup}-${v.moduleName}-${v.moduleVersion}-.*/) {
                    // This is the one
                    version = v.moduleVersion
                }
            }
        }
        if (!version) {
            println "ERROR: Could not find version: ${target}"
        }
        return version
    }

    Set<AndroidDependency> getLatestDependencies(Map<Project, Set<AndroidDependency>> dependencies, Set<AndroidDependency> projectDependencies) {
        Set<AndroidDependency> allDependencies = []
        for (Set<AndroidDependency> d : dependencies.values()) {
            d.each { AndroidDependency dependency ->
                allDependencies << dependency
            }
        }

        Set<AndroidDependency> latestDependencies = []
        projectDependencies.each { AndroidDependency dependency ->
            def dependencyGroup = getDependencyGroup(dependency.file)
            def dependencyName = getDependencyName(dependency.file)
            String latestJarVersion = "0"
            def duplicateDependencies = allDependencies.findAll {
                getDependencyGroup(it.file) == dependencyGroup && getDependencyName(it.file) == dependencyName
            }
            AndroidDependency latestDependency
            if (1 < duplicateDependencies.size()) {
                duplicateDependencies.each {
                    if (versionIsNewerThan(getVersionName(it.file), latestJarVersion)) {
                        latestJarVersion = getVersionName(it.file)
                    }
                }
                latestDependency = duplicateDependencies.find { getVersionName(it.file) == latestJarVersion }
            } else {
                latestJarVersion = getVersionName(dependency.file)
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
        def dependencyName = getDependencyName(dependency.file)
        def versionName = getVersionName(dependency.file)
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
            getDependencyName(it.file) == dependencyName && getVersionName(it.file) != versionName
        }.each { AndroidDependency androidDependency ->
            println "  Same dependency exists: ${dependencyFilename}, ${androidDependency.file.name}"
            sameDependencyExists = true
            def v1 = getVersionName(dependency.file)
            def v2 = getVersionName(androidDependency.file)
            // 'androidDependency.file' may be removed in previous loop
            if (androidDependency.file.exists() && versionIsNewerThan(v1, v2)) {
                println "  Found older dependency. Copy ${dependencyFilename} to all subprojects"
                isNewer = true
                // Should be replaced to jarFilename jar
                projects.each { Project pp ->
                    def projectLibDir = pp.file('libs')
                    if (isAarDependency) {
                        projectLibDir.listFiles().findAll {
                            it.isDirectory() && getDependencyName(it) == dependencyName
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
                            !it.isDirectory() && getDependencyName(it) == dependencyName
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

    static boolean versionIsNewerThan(String v1, String v2) {
        def cv1 = new ComparableVersion(v1)
        def cv2 = new ComparableVersion(v2)
        return cv2.compareTo(cv1) < 0
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
\t<classpathentry kind="src" path="gen"/>
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
</classpath>
"""
    }

    void generateEclipseProjectFile(Project p, AndroidDependency dependency) {
        def projectName = extension.projectName ?: p.name
        def name = dependency.getQualifiedName()
        p.file("${extension.aarDependenciesDir}/${name}/.project").text = """\
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
\t<name>${extension.projectNamePrefix}${projectName}-${name}</name>
\t<comment></comment>
\t<projects>
\t</projects>
\t<buildSpec>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ResourceManagerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.PreCompilerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ApkBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t</buildSpec>
\t<natures>
\t\t<nature>org.eclipse.jdt.core.javanature</nature>
\t\t<nature>com.android.ide.eclipse.adt.AndroidNature</nature>
\t</natures>
</projectDescription>
"""
    }

    void generateEclipseClasspathFileForParent(Project p) {
        // Use srcDirs definition for classpath entry
        def androidSrcDirs = p.android?.sourceSets?.main?.java?.srcDirs
        if (!androidSrcDirs) {
            androidSrcDirs = ['src/main/java']
        }
        if (!androidSrcDirs.contains('gen')) {
            androidSrcDirs << 'gen'
        }
        def androidSrcPaths = []
        androidSrcDirs.each {
            androidSrcPaths << (it.toString() - p.projectDir.path).replaceFirst("^[/\\\\]", '')
        }
        def classpathFile = p.file('.classpath')
        List<String> srcPaths = []
        List<String> libNames = []
        if (classpathFile.exists()) {
            // Aggregate src paths and dependencies
            def classPaths = new XmlSlurper().parseText(classpathFile.text)
            def srcPathEntries = classPaths.classpathentry?.findAll { it.@kind?.text() == 'src' }
            srcPaths = srcPathEntries.collect { it.@path.text() }
            def libClassPathEntries = classPaths.classpathentry?.findAll { it.@kind?.text() == 'lib' }
            libNames = libClassPathEntries.collect { it.@path.text().replaceFirst('^libs/', '') }
        } else {
            // Create minimum classpath file
            srcPaths = androidSrcPaths
            def srcPathEntries = androidSrcPaths.collect { """
\t<classpathentry kind="src" path="${it}"/>""" }.join('')
            classpathFile.text = """\
<?xml version="1.0" encoding="UTF-8"?>
<classpath>${srcPathEntries}
\t<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
\t<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
\t<classpathentry kind="output" path="bin/classes"/>
</classpath>
"""
        }

        androidSrcPaths = androidSrcPaths.findAll { srcPaths.find { path -> path == it} == null }
        if (androidSrcPaths) {
            def entriesToAdd = androidSrcPaths.collect { it -> "\t<classpathentry kind=\"src\" path=\"${it}\"/>" }
            def lines = classpathFile.readLines()?.findAll { it != '</classpath>' }
            lines += entriesToAdd
            lines += "</classpath>${System.getProperty('line.separator')}"
            classpathFile.text = lines.join(System.getProperty('line.separator'))
        }

        List<String> jars = jarDependencies[p].collect { "${it.getQualifiedName()}.jar" } + aarDependencies[p].collect {
            "${it.getQualifiedName()}.jar"
        }
        jars = jars.findAll { libNames.find { lib -> lib == it } == null }
        if (jars) {
            def entriesToAdd = jars.collect { it -> "\t<classpathentry kind=\"lib\" path=\"libs/${it}\"/>" }
            def lines = classpathFile.readLines()?.findAll { it != '</classpath>' }
            lines += entriesToAdd
            lines += "</classpath>${System.getProperty('line.separator')}"
            classpathFile.text = lines.join(System.getProperty('line.separator'))
        }
    }

    void generateEclipseProjectFileForParent(Project p) {
        def file = p.file(".project")
        if (file.exists()) {
            return
        }
        file.text = """\
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
\t<name>${p.name}</name>
\t<comment></comment>
\t<projects>
\t</projects>
\t<buildSpec>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ResourceManagerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.PreCompilerBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t\t<buildCommand>
\t\t\t<name>com.android.ide.eclipse.adt.ApkBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t</buildSpec>
\t<natures>
\t\t<nature>org.eclipse.jdt.core.javanature</nature>
\t\t<nature>com.android.ide.eclipse.adt.AndroidNature</nature>
\t</natures>
</projectDescription>
"""
    }

    void generateProjectPropertiesFileForParent(Project p) {
        def projectPropertiesFile = p.file('project.properties')
        List<String> libNames = []
        List<String> projectNames = []
        int maxReference = 0
        boolean shouldAddLibrary = false
        if (p.plugins.hasPlugin('com.android.library')) {
            shouldAddLibrary = true
        }
        if (projectPropertiesFile.exists()) {
            Properties props = new Properties()
            projectPropertiesFile.withInputStream { stream -> props.load(stream) }

            props.propertyNames().findAll {
                it =~ /^android\.library\.reference\.[0-9]+/
            }.each {
                Matcher mValue = props[it] =~ /^${extension.aarDependenciesDir}\\/(.*)/
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
            if (shouldAddLibrary && props.hasProperty('android.library')) {
                shouldAddLibrary = false
            }
        } else {
            // Create minimum properties file
            projectPropertiesFile.text = """\
target=${extension.androidTarget}
"""
        }

        def entriesToAdd = []
        if (shouldAddLibrary) {
            entriesToAdd << 'android.library=true'
        }

        List<String> list = projectDependencies[p]?.collect { it.getQualifiedName().replaceFirst('^:', '') }
        list = list?.findAll { projectNames.find { prj -> prj == it } == null }
        list?.each {
            maxReference++
            entriesToAdd << "android.library.reference.${maxReference}=../${it}"
        }

        List<String> aars = aarDependencies[p].collect { it.getQualifiedName() }
        aars = aars?.findAll { libNames.find { lib -> lib == it } == null }
        aars?.each {
            maxReference++
            entriesToAdd << "android.library.reference.${maxReference}=${extension.aarDependenciesDir}/${it}"
        }

        if (0 < entriesToAdd.size()) {
            def content = projectPropertiesFile.text
            if (!content.endsWith(System.getProperty('line.separator'))) {
                content += System.getProperty('line.separator')
            }
            projectPropertiesFile.text = content + entriesToAdd.join(System.getProperty('line.separator')) + System.getProperty('line.separator')
        }
    }
}
