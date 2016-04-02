package com.github.ksoichiro.eclipse.aar

import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.ResolvedModuleVersion

class DummyResolvedDependency implements ResolvedDependency {
    String moduleName
    String moduleGroup
    String moduleVersion

    @Override
    String getName() {
        return null
    }

    @Override
    String getModuleGroup() {
        return moduleGroup
    }

    @Override
    String getModuleName() {
        return moduleName
    }

    @Override
    String getModuleVersion() {
        return moduleVersion
    }

    @Override
    String getConfiguration() {
        return null
    }

    @Override
    ResolvedModuleVersion getModule() {
        return null
    }

    @Override
    Set<ResolvedDependency> getChildren() {
        return null
    }

    @Override
    Set<ResolvedDependency> getParents() {
        return null
    }

    @Override
    Set<ResolvedArtifact> getModuleArtifacts() {
        return null
    }

    @Override
    Set<ResolvedArtifact> getAllModuleArtifacts() {
        return null
    }

    @Override
    Set<ResolvedArtifact> getParentArtifacts(ResolvedDependency parent) {
        return null
    }

    @Override
    Set<ResolvedArtifact> getArtifacts(ResolvedDependency parent) {
        return null
    }

    @Override
    Set<ResolvedArtifact> getAllArtifacts(ResolvedDependency parent) {
        return null
    }
}
