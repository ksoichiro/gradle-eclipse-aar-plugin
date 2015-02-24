package com.github.ksoichiro.eclipse.aar

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction
import org.gradle.mvn3.org.apache.maven.artifact.versioning.ComparableVersion

import java.util.regex.Matcher

class GenerateTask extends BaseTask {
    Map<AndroidProject, Set<AndroidDependency>> fileDependencies
    Map<AndroidProject, Set<AndroidDependency>> projectDependencies
    Map<String, ResolvedDependency> allConfigurationsDependencies

    GenerateTask() {
        description = 'Used for Eclipse. Copies all AAR dependencies for library directory.'
    }

    @TaskAction
    def exec() {
        extension = project.eclipseAar
        fileDependencies = [:]
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
        projects.each { AndroidProject p ->
            androidConfigurations(p).each { Configuration configuration ->
                println "Aggregating resolved dependencies for project ${p.project.name} from ${configuration.name} configuration"
                aggregateResolvedDependencies(configuration.resolvedConfiguration.firstLevelModuleDependencies, "  ")
            }
        }
        projects.each { AndroidProject p ->
            androidConfigurations(p).each { Configuration configuration ->
                println "Classifying dependencies for project ${p.project.name} from ${configuration.name} configuration"
                configuration.each { File file ->
                    def convertedPath = file.path.tr(System.getProperty('file.separator'), '.')
                    def convertedPathExtStripped = convertedPath.lastIndexOf('.').with {
                        it != -1 ? convertedPath[0..<it] : convertedPath
                    }
                    def localDependency = configuration.dependencies.find { d -> convertedPathExtStripped.endsWith("${d.name}-release") }
                    if (localDependency instanceof ProjectDependency) {
                        // ProjectDependency should be not be exploded, just include in project.properties with relative path
                        AndroidProject dependencyProject = projects.find { it.project.name == ((ProjectDependency) localDependency).dependencyProject.name }
                        def d = new AndroidDependency()
                        d.with {
                            name = dependencyProject.project.name
                            artifactType = AndroidArtifactType.PROJECT
                        }
                        if (!projectDependencies[p]) {
                            projectDependencies[p] = [] as Set<AndroidDependency>
                        }
                        projectDependencies[p] << d
                    } else {
                        if (!fileDependencies[p]) {
                            fileDependencies[p] = [] as Set<AndroidDependency>
                        }
                        fileDependencies[p] << getDependencyFromFile(file)
                    }
                }
            }
        }
        projects.each { AndroidProject p ->
            fileDependencies[p] = getLatestDependencies(fileDependencies, fileDependencies[p])
        }

        projects.each { AndroidProject p ->
            fileDependencies[p].each { AndroidDependency d -> copyJarIfNewer(p, d) }
        }

        projects.each { AndroidProject p ->
            fileDependencies[p].findAll {
                it.artifactType == AndroidArtifactType.AAR
            }?.each { AndroidDependency d ->
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

    Collection<Configuration> androidConfigurations(AndroidProject p) {
        extension.targetConfigurations.collect { p.project.configurations.getByName(it) }
    }

    static String getBaseName(String filename) {
        filename.lastIndexOf('.').with { it != -1 ? filename[0..<it] : filename }
    }

    AndroidDependency getDependencyFromFile(File file) {
        // '-' is not always the separator of version, version itself often has '-'.
        // So we should find version from resolved dependencies.
        // Path: .../modules-2/files-2.1/group/artifact/version/hash/artifact-version.ext
        def baseFilename = getBaseName(file.name)
        String target = file.path.tr(System.getProperty('file.separator'), '-')
        AndroidDependency dependency = null
        allConfigurationsDependencies.each { k, v ->
            if ("${v.moduleName}-${v.moduleVersion}" == baseFilename) {
                // This may be the dependency of the file
                // Find group from file path and check qualified name matches
                if (target ==~ /.*-${v.moduleGroup}-${v.moduleName}-${v.moduleVersion}-.*/) {
                    // This is the one
                    dependency = new AndroidDependency()
                    dependency.with {
                        group = v.moduleGroup
                        name = v.moduleName
                        version = v.moduleVersion
                        it.file = file
                        artifactType = file.name.endsWith('jar') ? AndroidArtifactType.JAR : AndroidArtifactType.AAR
                    }
                }
            }
        }
        if (!dependency) {
            println "ERROR: Could not find dependency: ${target}"
        }
        return dependency
    }

    Set<AndroidDependency> getLatestDependencies(Map<AndroidProject, Set<AndroidDependency>> dependencies, Set<AndroidDependency> projectDependencies) {
        Set<AndroidDependency> allDependencies = []
        for (Set<AndroidDependency> d : dependencies.values()) {
            d.each { AndroidDependency dependency ->
                allDependencies << dependency
            }
        }

        Set<AndroidDependency> latestDependencies = []
        projectDependencies.each { AndroidDependency dependency ->
            String latestJarVersion = "0"
            def duplicateDependencies = allDependencies.findAll {
                dependency.isSameArtifact(getDependencyFromFile(it.file))
            }
            AndroidDependency latestDependency
            if (1 < duplicateDependencies.size()) {
                duplicateDependencies.each {
                    def d = getDependencyFromFile(it.file)
                    if (versionIsNewerThan(d?.version, latestJarVersion)) {
                        latestJarVersion = d?.version
                    }
                }
                latestDependency = duplicateDependencies.find { getDependencyFromFile(it.file)?.version == latestJarVersion }
            } else {
                latestJarVersion = dependency.version
                latestDependency = dependency
            }
            if (latestDependency) {
                latestDependency.version = latestJarVersion
                latestDependencies << latestDependency
            }
        }
        latestDependencies
    }

    void copyJarIfNewer(AndroidProject p, AndroidDependency dependency) {
        def dependencyProjectName = dependency.getQualifiedName()
        boolean isAarDependency = dependency.artifactType == AndroidArtifactType.AAR
        def copyClosure = isAarDependency ? { destDir ->
            p.project.copy { CopySpec it ->
                it.from p.project.zipTree(dependency.file)
                it.exclude 'classes.jar'
                it.into "${extension.aarDependenciesDir}/${dependencyProjectName}"
            }
            p.project.copy { CopySpec it ->
                it.from p.project.zipTree(dependency.file)
                it.include 'classes.jar'
                it.into destDir
                it.rename { String fileName ->
                    fileName.replace('classes.jar', "${dependencyProjectName}.jar")
                }
            }
        } : { destDir ->
            p.project.copy { CopySpec it ->
                it.from dependency.file
                it.into destDir
                it.rename { "${dependencyProjectName}.jar" }
            }
        }
        println "Adding dependency: ${dependency.file.path}"
        copyClosure('libs')
        if (isAarDependency) {
            p.project.copy { CopySpec it ->
                it.from p.project.zipTree(dependency.file)
                it.exclude 'classes.jar'
                it.into "${extension.aarDependenciesDir}/${dependencyProjectName}"
            }
            copyClosure("${extension.aarDependenciesDir}/${dependencyProjectName}/libs")
        }
    }

    static boolean versionIsNewerThan(String v1, String v2) {
        def cv1 = new ComparableVersion(v1)
        def cv2 = new ComparableVersion(v2)
        return cv2.compareTo(cv1) < 0
    }

    void generateProjectPropertiesFile(AndroidProject p, AndroidDependency dependency) {
        p.project.file("${extension.aarDependenciesDir}/${dependency.getQualifiedName()}/project.properties").text = """\
target=${extension.androidTarget}
android.library=true
"""
    }

    void generateEclipseClasspathFile(AndroidProject p, AndroidDependency dependency) {
        p.project.file("${extension.aarDependenciesDir}/${dependency.getQualifiedName()}/.classpath").text = """\
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

    void generateEclipseProjectFile(AndroidProject p, AndroidDependency dependency) {
        def projectName = extension.projectName ?: p.project.name
        def name = dependency.getQualifiedName()
        p.project.file("${extension.aarDependenciesDir}/${name}/.project").text = projectFileText("${extension.projectNamePrefix}${projectName}-${name}")
    }

    void generateEclipseClasspathFileForParent(AndroidProject p) {
        // Use srcDirs definition for classpath entry
        def androidSrcDirs = []
        p.project.android?.sourceSets?.findAll { it.name in ['main', 'debug'] }?.each {
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
            androidSrcPaths << (it.toString() - p.project.projectDir.path).replaceFirst("^[/\\\\]", '')
        }
        def classpathFile = p.project.file('.classpath')
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

        List<String> jars = fileDependencies[p].collect { "${it.getQualifiedName()}.jar" }
        jars = jars.findAll { libNames.find { lib -> lib == it } == null }
        if (jars) {
            def entriesToAdd = jars.collect { it -> "\t<classpathentry kind=\"lib\" path=\"libs/${it}\"/>" }
            def lines = classpathFile.readLines()?.findAll { it != '</classpath>' }
            lines += entriesToAdd
            lines += "</classpath>${System.getProperty('line.separator')}"
            classpathFile.text = lines.join(System.getProperty('line.separator'))
        }
    }

    void generateEclipseProjectFileForParent(AndroidProject p) {
        def file = p.project.file(".project")
        if (file.exists()) {
            return
        }
        def projectName = extension.projectName ?: p.project.name
        file.text = projectFileText("${extension.projectNamePrefix}${projectName}")
    }

    void generateProjectPropertiesFileForParent(AndroidProject p) {
        def projectPropertiesFile = p.project.file('project.properties')
        List<String> libNames = []
        List<String> projectNames = []
        int maxReference = 0
        boolean shouldAddLibrary = false
        if (p.project.plugins.hasPlugin('com.android.library')) {
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
            if (shouldAddLibrary && props.containsKey('android.library')) {
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

        List<String> aars = fileDependencies[p].findAll { it.artifactType == AndroidArtifactType.AAR }?.collect { it.getQualifiedName() }
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

    static String projectFileText(String projectName) {
        """\
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
\t<name>${projectName}</name>
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
}
