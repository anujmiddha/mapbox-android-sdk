[![Build Status](https://travis-ci.org/mapbox/mapbox-android-sdk.svg?branch=master)](https://travis-ci.org/mapbox/mapbox-android-sdk)

# Mapbox Android SDK

An open source alternative for native maps on Android. This library lets
you use [Mapbox](https://www.mapbox.com/), [OpenStreetMap](http://www.openstreetmap.org/),
and other tile sources in your app, as well as overlays like [GeoJSON](http://geojson.org/)
data and interactive tooltips.

This is a fork of [osmdroid](http://code.google.com/p/osmdroid/), so the entire
core is open source: it doesn't depend on the Google Maps SDK or any components
outside of AOSP that would require the Google Play Store.

## Installation

We recommend using the Mapbox Android SDK with [Gradle](http://www.gradle.org/):
this will automatically install the necessary dependencies and pull the SDK
binaries from the Maven Central repository ( [Mapbox Android SDK on Maven Central](http://search.maven.org/#artifactdetails%7Ccom.mapbox.mapboxsdk%7Cmapbox-android-sdk%7C0.2.3%7Cjar) ).


### With Gradle (Android Studio, IntelliJ, etc)

Add this to your to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile ('com.mapbox.mapboxsdk:mapbox-android-sdk:0.2.3@aar'){
        transitive=true
    }
}
```

### Manually / Hardcoding In Project

Download and include the mapbox-android-sdk.aar file and all
artifacts (.aar and .jar files listed) listed in `MapboxAndroidSDK / build.gradle`.
These **will** change over time so please check back regularly.

*Example:*

* Mapbox Android SDK (.aar) - 0.2.3
* Android Support V4 - 19.1
* OkHttp - 1.3.0
* NineOldAndroids - 2.4.0
* DiskLRUCache - 2.0.1
* Guava - 16.0.1
 
### Legacy Support (Eclipse) - Experimental

The Mapbox Android SDK is also packaged as a `.apk` file.  This allows integration with older tools (Eclipse) that don't support the `.aar` format yet.  It's also available from Maven Central via:

```xml
<dependency> 
    <groupId>com.mapbox.mapboxsdk</groupId> 
    <artifactId>mapbox-android-sdk</artifactId> 
    <version>0.2.3</version> 
    <type>apklib</type> 
</dependency>
```

### Building From Source

Building from source means you get the very latest version of our code.
The first step is to clone the repository to a directory in your system

    git clone https://github.com/mapbox/mapbox-android-sdk.git

We use Gradle as a configuration and build tool: to use it with your IDE,
import the project by selecting `build.gradle` in the project root directory
as the project file.

Don't worry about installing Gradle on your system if you don't already have
it:  the project makes use of Gradle Wrapper, so a correct & current project
version of Gradle will automatically be installed and used to run the builds.
To use the Gradle wrapper just look for `gradlew`  or `gradlew.bat` (Windows)
in the project's main directory.  For example:

    cd <PROJECT_ROOT>
     ./gradlew --version

which will produce something like:

```
------------------------------------------------------------
Gradle 1.10
------------------------------------------------------------

Build time:   2013-12-17 09:28:15 UTC
Build number: none
Revision:     36ced393628875ff15575fa03d16c1349ffe8bb6

Groovy:       1.8.6
Ant:          Apache Ant(TM) version 1.9.2 compiled on July 8 2013
Ivy:          2.2.0
JVM:          1.7.0_40 (Oracle Corporation 24.0-b56)
OS:           Mac OS X 10.9.2 x86_64
```

See the [Gradle Wrapper documentation for more details](http://www.gradle.org/docs/current/userguide/gradle_wrapper.html).

Then you can build an archive:

```sh
./gradlew clean assembleRelease

# The archive (mapbox-android-sdk-<VERSION>.aar) will be found in
<PROJECTHOME>/MapboxAndroidSDK/build/libs
```

**Don't forget to then also include the dependencies from `MapboxAndroidSDK / build.gradle` in your classpath!**

## [Quick-start Guide](https://github.com/mapbox/mapbox-android-sdk/blob/master/QUICKSTART.md)
