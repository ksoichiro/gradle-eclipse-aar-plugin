# Releasing

## Update version

* gradle.properties
* README.md

## Clean, build, upload to Sonatype

```
./gradlew clean build uploadArchives -Prelease
```

## Release

Publish uploaded files at oss.sonatype.org.
