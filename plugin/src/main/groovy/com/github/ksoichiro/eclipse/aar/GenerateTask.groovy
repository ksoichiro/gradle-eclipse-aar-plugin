package com.github.ksoichiro.eclipse.aar

import com.github.ksoichiro.eclipse.aar.generator.*
import org.apache.maven.artifact.versioning.ComparableVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction

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
                configuration.fileCollection {
                    // Exclude file dependency
                    it instanceof ProjectDependency || !(it instanceof SelfResolvingDependency)
                }.each { File file ->
                    def convertedPath = file.path.tr(System.getProperty('file.separator'), '.')
                    def convertedPathExtStripped = convertedPath.lastIndexOf('.').with {
                        it != -1 ? convertedPath[0..<it] : convertedPath
                    }
                    def localDependency = configuration.dependencies.find { d -> convertedPathExtStripped.endsWith("${d.name}-release") }
                    if (localDependency instanceof ProjectDependency) {
                        // ProjectDependency should be not be exploded, just include in project.properties with relative path
                        AndroidProject dependencyProject = projects.find {
                            it.project.name == ((ProjectDependency) localDependency).dependencyProject.name
                        }
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
        if (extension.jarDependenciesDir) {
            projects.findAll { AndroidProject p ->
                p.project.file(extension.jarDependenciesDir).exists()
            }.each { AndroidProject p ->
                p.project.file(extension.jarDependenciesDir).listFiles().findAll { File file ->
                    file.name.endsWith('.jar')
                }.each { File file ->
                    def dependency = new AndroidDependency()
                    dependency.with {
                        it.file = file
                        artifactType = AndroidArtifactType.RAW_JAR
                    }
                    fileDependencies[p] << dependency
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
                generateRequiredDirectories(p, d)
                generateProjectPropertiesFile(p, d)
                generateEclipseClasspathFile(p, d)
                generateEclipseProjectFile(p, d)
            }
            generateRequiredDirectories(p)
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
            d.findAll {
                null != it && !it.isRawJar()
            }.each { AndroidDependency dependency ->
                allDependencies << dependency
            }
        }

        Set<AndroidDependency> latestDependencies = []
        projectDependencies.findAll { it != null }?.each { AndroidDependency dependency ->
            if (dependency.isRawJar()) {
                latestDependencies << dependency
            } else {
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
                    latestDependency = duplicateDependencies.find {
                        getDependencyFromFile(it.file)?.version == latestJarVersion
                    }
                } else {
                    latestJarVersion = dependency.version
                    latestDependency = dependency
                }
                if (latestDependency) {
                    latestDependency.version = latestJarVersion
                    latestDependencies << latestDependency
                }
            }
        }
        latestDependencies
    }

    void copyJarIfNewer(AndroidProject p, AndroidDependency dependency) {
        String dependencyProjectRootPath = dependencyProjectRootPath(dependency)
        String dependencyProjectName = dependency.getQualifiedName()
        boolean isAarDependency = dependency.artifactType == AndroidArtifactType.AAR
        def copyClosure = isAarDependency ? { destDir ->
            p.project.copy { CopySpec it ->
                it.from p.project.zipTree(dependency.file)
                it.exclude 'classes.jar'
                it.into dependencyProjectRootPath
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
                it.into dependencyProjectRootPath
            }
            copyClosure("${dependencyProjectRootPath}/libs")
        }
    }

    static boolean versionIsNewerThan(String v1, String v2) {
        def cv1 = new ComparableVersion(v1)
        def cv2 = new ComparableVersion(v2)
        return cv2.compareTo(cv1) < 0
    }

    void generateRequiredDirectories(AndroidProject p, AndroidDependency dependency) {
        new OutputDirGenerator().generate(dependencyProjectRoot(p, dependency))
    }

    void generateProjectPropertiesFile(AndroidProject p, AndroidDependency dependency) {
        new ProjectPropertiesFileGenerator(
                androidTarget: extension.androidTarget)
                .generate(new File(dependencyProjectRoot(p, dependency), 'project.properties'))
    }

    void generateEclipseClasspathFile(AndroidProject p, AndroidDependency dependency) {
        new ClasspathFileGenerator(
                andmore: extension.andmore)
                .generate(new File(dependencyProjectRoot(p, dependency), '.classpath'))
    }

    void generateEclipseProjectFile(AndroidProject p, AndroidDependency dependency) {
        def projectName = extension.projectName ?: p.project.name
        def name = dependency.getQualifiedName()
        new ProjectFileGenerator(
                projectName: "${extension.projectNamePrefix}${projectName}-${name}",
                andmore: extension.andmore)
                .generate(p.project.file("${extension.aarDependenciesDir}/${name}/.project"))
    }

    static void generateRequiredDirectories(AndroidProject p) {
        new OutputDirGenerator().generate(p.project.projectDir)
    }

    void generateEclipseClasspathFileForParent(AndroidProject p) {
        new ParentClasspathFileGenerator(
                project: p.project,
                andmore: extension.andmore)
                .generate(p.project.file('.classpath'))
    }

    void generateEclipseProjectFileForParent(AndroidProject p) {
        def projectName = extension.projectName ?: p.project.name
        new ProjectFileGenerator(
                projectName: "${extension.projectNamePrefix}${projectName}",
                andmore: extension.andmore)
                .generate(p.project.file(".project"))
    }

    void generateProjectPropertiesFileForParent(AndroidProject p) {
        new ParentProjectPropertiesFileGenerator(
                project: p.project,
                androidTarget: extension.androidTarget,
                aarDependenciesDir: extension.aarDependenciesDir,
                fileDependencies: fileDependencies[p],
                projectDependencies: projectDependencies[p])
                .generate(p.project.file('project.properties'))
    }

    File dependencyProjectRoot(AndroidProject p, AndroidDependency dependency) {
        p.project.file(dependencyProjectRootPath(dependency))
    }

    String dependencyProjectRootPath(AndroidDependency dependency) {
        "${extension.aarDependenciesDir}/${dependency.getQualifiedName()}"
    }
}
