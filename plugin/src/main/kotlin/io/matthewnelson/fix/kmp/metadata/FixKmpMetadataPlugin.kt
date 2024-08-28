/*
 * Copyright (c) 2024 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.matthewnelson.fix.kmp.metadata

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import java.io.File

public class FixKmpMetadataPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        require(target.rootProject == target) {
            "${this::class.simpleName} must be applied to the root project"
        }

        target.allprojects(FixUniqueName)
    }

    private object FixUniqueName: Action<Project> {
        override fun execute(t: Project) = with (t) {
            tasks.withType(KotlinCompileTool::class.java).all(InterceptTasks(t))
        }
    }

    private class InterceptTasks(private val project: Project): Action<KotlinCompileTool> {

        override fun execute(t: KotlinCompileTool) = with(t) {
            if (!name.startsWith("compile")) return@with
            if (!name.endsWith("MainKotlinMetadata")) return@with

            doLast {
                val groupId = groupId()

                outputs.files.files.flatMap { output ->
                    output.walkTopDown().mapNotNull { file ->
                        file.takeIf { it.isFile && it.name == "manifest" }
                    }
                }.forEach { manifest -> manifest.fixUniqueName(groupId) }
            }
        }

        private fun groupId(): String {
            val groupId = project.group.toString()

            require(groupId.isNotBlank()) {
                "Project.group is not set for ${project.path}"
            }
            require(groupId.indexOfFirst { it.isWhitespace() } == -1) {
                "Project.group contains whitespace for ${project.path}"
            }
            return groupId
        }

        private fun File.fixUniqueName(groupId: String) {
            val content = readLines().let { lines ->
                val map = LinkedHashMap<String, String>(lines.size, 1.0f)
                for (line in lines) {
                    if (line.isBlank()) continue

                    val iEq = line.indexOf('=')
                    require(iEq != -1) {
                        "Metadata manifest file contents invalid." +
                        " Contains invalid key-value-pair '$line'"
                    }

                    map[line.substring(0, iEq)] = line.substring(iEq + 1)
                }
                map
            }

            val old = content[KEY_UNIQUE_NAME] ?: return

            val prefix = "$groupId\\:"

            // If there's already a prefix, this source set does not need fixing.
            if (old.startsWith(prefix)) return

            val new = "$prefix$old"
            content[KEY_UNIQUE_NAME] = new

            bufferedWriter().use { writer ->
                content.entries.forEach { (key, value) ->
                    writer.write(key)
                    writer.write("=")
                    writer.write(value)
                    writer.newLine()
                }
            }

            if (project.properties[KEY_SILENCE] != "true") {
                println("""
                    Kotlin Metadata manifest 'unique_name' field fixed.
                    Old[$old] >> New[$new]
                    This message can be silenced by adding '$KEY_SILENCE=true' to gradle.properties
                """.trimIndent())
            }
        }

        private companion object {
            private const val KEY_SILENCE = "io.matthewnelson.silenceFixKmpMetadata"
            private const val KEY_UNIQUE_NAME = "unique_name"
        }
    }
}
