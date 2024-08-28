/*
 * Copyright (c) 2024 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
plugins {
    id("shared")
    id("java-gradle-plugin")
}

gradlePlugin {
    plugins {
        create("io.matthewnelson.fix.kmp.metadata") {
            id = "io.matthewnelson.fix.kmp.metadata"
            implementationClass = "io.matthewnelson.fix.kmp.metadata.FixKmpMetadataPlugin"
            displayName = "Gradle Fix Kotlin Multiplatform Metadata Plugin"
            description = "Gradle plugin that fixes intermediate source set Metadata for Kotlin Multiplatform"
        }
    }
}

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(libs.gradle.kotlin)
}

if (!findProperty("VERSION_NAME")!!.toString().endsWith("SNAPSHOT")) {
    signing {
        useGpgCmd()
    }
}
