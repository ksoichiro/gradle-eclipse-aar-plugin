package com.github.ksoichiro.eclipse.aar

import static com.github.ksoichiro.eclipse.aar.AndroidArtifactType.AAR
import static com.github.ksoichiro.eclipse.aar.AndroidArtifactType.JAR
import static com.github.ksoichiro.eclipse.aar.AndroidArtifactType.RAW_JAR

class AndroidDependencySpec extends BaseSpec {
    def "getQualifiedName"() {
        expect:
        result == new AndroidDependency(group: g, name: n, version: v, artifactType: a, file: f).getQualifiedName()

        where:
        g | n | v | a | f || result
        'com.example.foo' | 'bar' | '1.0.0' | JAR     | new File('/path/to/file/bar-1.0.0.jar') || 'com.example.foo-bar-1.0.0'
        null              | 'bar' | '1.0.0' | JAR     | new File('/path/to/file/bar-1.0.0.jar') || 'bar-1.0.0'
        'com.example.foo' | null  | '1.0.0' | JAR     | new File('/path/to/file/bar-1.0.0.jar') || 'com.example.foo-1.0.0'
        'com.example.foo' | 'bar' | null    | JAR     | new File('/path/to/file/bar-1.0.0.jar') || 'com.example.foo-bar'
        null              | null  | null    | JAR     | new File('/path/to/file/bar-1.0.0.jar') || 'bar-1.0.0'
        null              | null  | null    | JAR     | null                                    || ''
        null              | null  | null    | RAW_JAR | new File('/path/to/file/bar-1.0.0.jar') || 'bar-1.0.0'
    }

    def "isSameArtifact"() {
        expect:
        result == new AndroidDependency(group: g1, name: n1, version: v1, artifactType: a1)
                .isSameArtifact(new AndroidDependency(group: g2, name: n2, version: v2, artifactType: a2))

        where:
        g1                | n1    | v1      | a1   | g2                 | n2     | v2            | a2   || result
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0'         | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1'           | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.1'       | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0-alpha' | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | null          | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'blur' | '1.0.0'       | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'blur' | '1.0'         | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'blur' | '1'           | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'blur' | '1.0.1'       | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'blur' | '1.0.0-alpha' | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'baz'  | '1.0.0-alpha' | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | null   | '1.0.0'       | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.blur' | 'bar'  | '1.0.0'       | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.blur' | 'bar'  | '1.0'         | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.blur' | 'bar'  | '1'           | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.blur' | 'bar'  | '1.0.1'       | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.blur' | 'bar'  | '1.0.0-alpha' | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.baz'  | 'bar'  | '1.0.0'       | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | null               | 'bar'  | '1.0.0'       | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0'         | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1'           | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.1'       | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0-alpha' | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | null || false
        null              | 'bar' | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | JAR  || false
        'com.example.foo' | null  | '1.0.0' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | JAR  || false
        'com.example.foo' | 'bar' | null    | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null | 'com.example.foo'  | 'bar'  | '1.0.0'       | JAR  || false
        null              | 'bar' | '1.0.0' | JAR  | null               | 'bar'  | '1.0.0'       | JAR  || true
        'com.example.foo' | null  | '1.0.0' | JAR  | 'com.example.foo'  | null   | '1.0.0'       | JAR  || true
        'com.example.foo' | 'bar' | null    | JAR  | 'com.example.foo'  | 'bar'  | null          | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null | 'com.example.foo'  | 'bar'  | '1.0.0'       | null || true
    }

    def "isSameArtifact with null"() {
        expect:
        false == new AndroidDependency(group: 'com.example.foo', name: 'bar', version: '1.0.0', artifactType: JAR).isSameArtifact(null)
    }

    def "isSameArtifactVersion"() {
        expect:
        result == new AndroidDependency(group: 'com.example.foo', name: 'bar', version: v1)
                .isSameArtifactVersion(new AndroidDependency(group: 'com.example.foo', name: 'bar', version: v2))

        where:
        v1      | v2            || result
        '1.0.0' | '1.0.0'       || true
        '1.0.0' | '1.0'         || false
        '1.0.0' | '1'           || false
        '1.0.0' | '1.0.1'       || false
        '1.0.0' | '1.0.0-alpha' || false
    }
}
