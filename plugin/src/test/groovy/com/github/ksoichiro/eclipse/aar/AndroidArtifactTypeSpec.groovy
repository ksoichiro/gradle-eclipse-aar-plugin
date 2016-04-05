package com.github.ksoichiro.eclipse.aar

import spock.lang.Specification

class AndroidArtifactTypeSpec extends Specification {
    def "access to all values for coverage"() {
        when:
        AndroidArtifactType.values().each {
            AndroidArtifactType.valueOf(it.name())
        }

        then:
        notThrown(Exception)
    }
}
