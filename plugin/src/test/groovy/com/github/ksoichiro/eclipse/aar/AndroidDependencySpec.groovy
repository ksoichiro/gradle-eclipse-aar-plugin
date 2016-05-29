package com.github.ksoichiro.eclipse.aar

import static com.github.ksoichiro.eclipse.aar.AndroidArtifactType.AAR
import static com.github.ksoichiro.eclipse.aar.AndroidArtifactType.JAR
import static com.github.ksoichiro.eclipse.aar.AndroidArtifactType.RAW_JAR

class AndroidDependencySpec extends BaseSpec {
    def "getQualifiedName"() {
        expect:
        result == new AndroidDependency(group: g, name: n, version: v, classifier: c, artifactType: a, file: f).getQualifiedName()

        where:
        g | n | v | c | a | f || result
        'com.example.foo' | 'bar' | '1.0.0' | 'staging' | JAR     | new File('/path/to/file/bar-1.0.0-staging.jar') || 'com.example.foo-bar-1.0.0-staging'
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR     | new File('/path/to/file/bar-1.0.0.jar')         || 'com.example.foo-bar-1.0.0'
        null              | 'bar' | '1.0.0' | null      | JAR     | new File('/path/to/file/bar-1.0.0.jar')         || 'bar-1.0.0'
        'com.example.foo' | null  | '1.0.0' | null      | JAR     | new File('/path/to/file/bar-1.0.0.jar')         || 'com.example.foo-1.0.0'
        'com.example.foo' | 'bar' | null    | null      | JAR     | new File('/path/to/file/bar-1.0.0.jar')         || 'com.example.foo-bar'
        null              | null  | null    | null      | JAR     | new File('/path/to/file/bar-1.0.0.jar')         || 'bar-1.0.0'
        null              | null  | null    | null      | JAR     | null                                            || ''
        null              | null  | null    | null      | RAW_JAR | new File('/path/to/file/bar-1.0.0.jar')         || 'bar-1.0.0'
    }

    def "isSameArtifact"() {
        expect:
        result == new AndroidDependency(group: g1, name: n1, version: v1, classifier: c1, artifactType: a1)
                .isSameArtifact(new AndroidDependency(group: g2, name: n2, version: v2, classifier: c2, artifactType: a2))

        where:
        g1                | n1    | v1      | c1        | a1   | g2                 | n2     | v2            | c2        | a2   || result
        'com.example.foo' | 'bar' | '1.0.0' | 'staging' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | 'staging' | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | 'staging' | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0-alpha' | 'staging' | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | 'staging' | AAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | 'prod'    | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | 'staging' | AAR  | 'com.example.foo'  | 'bar'  | '1.0.0-alpha' | 'prod'    | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | 'prod'    | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | null      | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0'         | null      | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1'           | null      | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.1'       | null      | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0-alpha' | null      | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | null          | null      | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'blur' | '1.0.0'       | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'blur' | '1.0'         | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'blur' | '1'           | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'blur' | '1.0.1'       | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'blur' | '1.0.0-alpha' | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'baz'  | '1.0.0-alpha' | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | null   | '1.0.0'       | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.blur' | 'bar'  | '1.0.0'       | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.blur' | 'bar'  | '1.0'         | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.blur' | 'bar'  | '1'           | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.blur' | 'bar'  | '1.0.1'       | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.blur' | 'bar'  | '1.0.0-alpha' | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.baz'  | 'bar'  | '1.0.0'       | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | null               | 'bar'  | '1.0.0'       | null      | JAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | null      | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0'         | null      | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1'           | null      | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.1'       | null      | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0-alpha' | null      | AAR  || false
        'com.example.foo' | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | null      | null || false
        null              | 'bar' | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | null      | JAR  || false
        'com.example.foo' | null  | '1.0.0' | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | null      | JAR  || false
        'com.example.foo' | 'bar' | null    | null      | JAR  | 'com.example.foo'  | 'bar'  | '1.0.0'       | null      | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null      | null | 'com.example.foo'  | 'bar'  | '1.0.0'       | null      | JAR  || false
        null              | 'bar' | '1.0.0' | null      | JAR  | null               | 'bar'  | '1.0.0'       | null      | JAR  || true
        'com.example.foo' | null  | '1.0.0' | null      | JAR  | 'com.example.foo'  | null   | '1.0.0'       | null      | JAR  || true
        'com.example.foo' | 'bar' | null    | null      | JAR  | 'com.example.foo'  | 'bar'  | null          | null      | JAR  || true
        'com.example.foo' | 'bar' | '1.0.0' | null      | null | 'com.example.foo'  | 'bar'  | '1.0.0'       | null      | null || true
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
