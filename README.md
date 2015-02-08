# gradle-eclipse-aar-plugin

[![Build Status](http://img.shields.io/travis/ksoichiro/gradle-eclipse-aar-plugin.svg?style=flat&branch=master)](https://travis-ci.org/ksoichiro/gradle-eclipse-aar-plugin)

Gradle plugin to use Android AAR libraries on Eclipse.

You can manage dependencies with Gradle and build app on Eclipse.

## Prerequisites

* [Eclipse IDE for Java Developers 4.4 (Luna) SR1](https://eclipse.org/downloads/packages/eclipse-ide-java-developers/lunasr1a)
* [Eclipse ADT Plugin](http://developer.android.com/sdk/installing/installing-adt.html)
* Oracle JDK 7

## Usage

### Prepare build.gradle

```groovy
buildscript {
    repositories {
        mavenCentral()
        maven {
            url uri('https://oss.sonatype.org/content/repositories/snapshots/')
        }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:1.0.0'
        classpath 'com.github.ksoichiro:gradle-eclipse-aar-plugin:0.1.0-SNAPSHOT'
    }
}

apply plugin: 'com.android.application'
apply plugin: 'com.github.ksoichiro.eclipse.aar'

repositories {
    mavenCentral()
}

dependencies {
    compile 'com.android.support:appcompat-v7:21.0.2'
    compile 'com.nineoldandroids:library:2.4.0'
    compile 'com.melnykov:floatingactionbutton:1.0.7'
    compile 'com.github.ksoichiro:android-observablescrollview:1.5.0'
}

// Configure plugin
eclipseAar {
    androidTarget = 'android-21'
    aarDependenciesDir = 'aarDependencies'
}

// Configure android plugin
// (Even if you don't develop with Gradle, write following at least)
android {
    compileSdkVersion 1
}
```

### Prepare Gradle or Gradle wrapper

Install Gradle 2.2.1+.

Or copy Gradle wrapper files into your project.
If you use Gradle wrapper, you don't have to install Gradle.

* `gradle`
* `gradlew.sh`
* `gradlew.bat`

### Configure classpath

Add dependencies to Eclipse classpath entries (`.classpath`).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" path="src"/>
	<classpathentry kind="src" path="gen"/>
	<classpathentry kind="con" path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>
	<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.LIBRARIES"/>
	<classpathentry exported="true" kind="con" path="com.android.ide.eclipse.adt.DEPENDENCIES"/>
	<classpathentry exported="true" kind="con" path="org.springsource.ide.eclipse.gradle.classpathcontainer"/>
	<classpathentry kind="output" path="bin/classes"/>

	<!-- Define your dependencies: libs/ARTIFACT_ID-VERSION.jar -->
	<classpathentry kind="lib" path="libs/android-observablescrollview-1.5.0.jar"/>
	<classpathentry kind="lib" path="libs/appcompat-v7-21.0.2.jar"/>
	<classpathentry kind="lib" path="libs/floatingactionbutton-1.0.7.jar"/>
	<classpathentry kind="lib" path="libs/library-2.4.0.jar"/>
	<classpathentry kind="lib" path="libs/recyclerview-v7-21.0.0.jar"/>
	<classpathentry kind="lib" path="libs/support-annotations-21.0.2.jar"/>
	<classpathentry kind="lib" path="libs/support-v4-21.0.2.jar"/>
</classpath>
```

This will be generated automatically in the future.

### Configure project properties

```
target=android-21

# Define your dependencies: aarDependencies/ARTIFACT_ID-VERSION
android.library.reference.1=aarDependencies/android-observablescrollview-1.5.0
android.library.reference.2=aarDependencies/support-v4-21.0.2
android.library.reference.3=aarDependencies/floatingactionbutton-1.0.7
android.library.reference.4=aarDependencies/recyclerview-v7-21.0.0
android.library.reference.5=aarDependencies/appcompat-v7-21.0.2
```

This will be generated automatically in the future.

### Generate dependencies

```sh
$ ./gradlew generateEclipseDependencies
```

JAR dependencies will be copied to `libs` directory,  
and AAR dependencies will be exploded and copied to `aarDependencies` directory by default.

### Import projects to Eclipse and build app

1. Launch Eclipse.
1. Select `File` > `Import`.
1. Select `General` > `Existing Projects into Workspace` and click `Next`.
1. Click `Browse` and select project root directory.
1. Check `Search` for nested projects.
1. Select all projects and click next.
1. Some warning messages will be generated, but ignore them and wait until build finishes.

### Run the app

1. Confirm your device is connected.
1. Right click your main project and select `Run As` > `Android Application`.

## License

    Copyright 2015 Soichiro Kashima

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

