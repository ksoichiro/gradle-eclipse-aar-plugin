package com.github.ksoichiro.eclipse.aar

class AndroidDependency {
    static final String SEPARATOR = '-'

    String group
    String name
    String version
    File file
    AndroidArtifactType artifactType

    String getQualifiedName() {
        if (isProject()) {
            return name;
        }
        if (!group && !name && !version) {
            return file.name;
        }
        [group ?: "", name ?: "", version ?: ""].join(SEPARATOR)
    }

    boolean isProject() {
        artifactType == AndroidArtifactType.PROJECT
    }

    boolean isSameArtifact(AndroidDependency dependency) {
        group == dependency.group && name == dependency.name
    }

    boolean isSameArtifactVersion(AndroidDependency dependency) {
        group == dependency.group && name == dependency.name && version == dependency.version
    }
}
