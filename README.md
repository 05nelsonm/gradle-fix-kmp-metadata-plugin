# gradle-fix-kmp-metadata-plugin

A Gradle Plugin that Metadata for Kotlin Multiplatform.

### The Problem

Metadata is generated for all intermediate source sets for each project module within a 
Kotlin Multiplatform project. **But** that metadata manifest `unique_name` value is sometimes 
**not unique**. This is not normally an issue, but with Kotlin `2.0.0` and the new K2 compiler 
being the default, it causes problems with dependency resolution (See [KT-66568][url-kt-66568]).

Run the following gradle task
```bash
$ ./gradlew metadataCommonMainClasses
```

Now have a look at the generated metadata in `build/classes/kotlin/metadata` for each
intermediate source set and view the `manifest` file; the `unique_name` value should actually 
be **unique**.

Take the example of a project module named `runtime`. The output of `metadataCommonMainClasses` 
will produce the following metadata `manifest` file for `commonMain`.

```none
abi_version=1.8.0
compiler_version=1.9.24
ir_signature_versions=1,2
metadata_version=1.4.1
unique_name=runtime_commonMain
```

That's not unique... This presents a problem for the K2 compiler because if you have a dependency
(or multiple) which also uses the name `runtime` (such as compose, or sqldelight), then 
there will be a conflict which the `KLIB resolver` will warn you about. The `KLIB resolver` (until 
it is fixed) will only include the first `runtime_commonMain` dependency in the project and skip 
all the others. Just as alarming, the `unique_name` value for non-standard intermediate source sets 
(such as `nonJsMain`), are also affected.

### The Fix

<!-- TAG_VERSION -->

 1) Add this plugin to your root project's `build.gradle.kts` file
    ```kotlin
    plugins {
        id("io.matthewnelson.fix.kmp.metadata") version("0.1.0")
    }
    ``` 
 2) Run `$ ./gradlew metadataCommonMainClasses`
 3) See log output indicating fixed metadata manifest files.

**NOTE:** Library authors should ensure all their project module's `group` field is 
set to their publication group, as that is the field for which kotlin (and this plugin) 
uses when creating the prefix for the `unique_name` value.

e.g (root project's `build.gradle.kts` file)
```kotlin
allProjects {
    // My publication's group coordinates
    group = "io.matthewnelson.kmp-tor"
}
```

### Why a plugin for this?

Unless this fix for the `unique_name` value is backported to earlier versions of Kotlin,
projects using the K2 compiler may always have problems when consuming dependencies that 
do not provide a `commonMain` dependency where the `unique_name` is **actually** unique. 
This plugin provides a simple way for projects and library authors to avoid all of that 
while not being forced to upgrade their Kotlin version (if this gets fixed).

[url-kt-66568]: https://youtrack.jetbrains.com/issue/KT-66568/
