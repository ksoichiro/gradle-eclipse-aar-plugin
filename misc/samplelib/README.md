# Sample library

This project is just for providing file dependency (*.jar) for testing.
The content is meaningless.

## Build

```sh
$ ./gradlew :misc:samplelib:build
```

## Upload to local repository

```sh
$ ./gradlew :misc:samplelib:uploadArchives
```

Then you can use `com.github.ksoichiro:samplelib:1.0.0` with setting `misc/repo` as a repository:

```groovy
repositories {
    maven {
        url uri('misc/repo')
    }
}
```
