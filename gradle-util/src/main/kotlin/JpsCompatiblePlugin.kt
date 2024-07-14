/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.pill

import EmbeddedComponents
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.project
import java.io.File

open class PillExtension {
    /*
     * Here's how you can specify a custom variant:
     * `./gradlew pill -Dpill.variant=<NAME>`
     */
    enum class Variant {
        BASE, // Includes compiler and IDE (default)
        FULL, // Includes compiler, IDE and Gradle plugin
    }

    open var variant: Variant? = null

    open var excludedDirs: List<File> = emptyList()

    @Suppress("unused")
    fun Project.excludedDirs(vararg dirs: String) {
        excludedDirs = excludedDirs + dirs.map { File(projectDir, it) }
    }

    @Suppress("unused")
    fun serialize() = mapOf<String, Any?>(
        "variant" to variant?.name,
        "excludedDirs" to excludedDirs
    )
}

fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

private fun Task.useAndroidConfiguration(systemPropertyName: String, configName: String) {
    val configuration = with(project) {
        configurations.getOrCreate(configName)
            .also {
                if (it.allDependencies.matching { dep ->
                        dep is ProjectDependency &&
                                dep.targetConfiguration == configName &&
                                dep.dependencyProject.path == ":dependencies:android-sdk"
                    }.isEmpty()) {
                    dependencies.add(
                        configName,
                        dependencies.project(":dependencies:android-sdk", configuration = configName)
                    )
                }
            }
    }

    dependsOn(configuration)

    if (this is Test) {
        val androidFilePath = configuration.singleFile.canonicalPath
        doFirst {
            systemProperty(systemPropertyName, androidFilePath)
        }
    }
}

object TaskUtils {
    fun useAndroidSdk(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.sdk", configName = "androidSdk")
    }

    fun useAndroidJar(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.jar", configName = "androidJar")
    }

    fun useAndroidEmulator(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.sdk", configName = "androidEmulator")
    }
}

@Suppress("unused")
class JpsCompatiblePlugin : Plugin<Project> {
    override fun apply(project: Project) {
//        project.configurations.maybeCreate(EmbeddedComponents.CONFIGURATION_NAME)
//        project.extensions.create("pill", PillExtension::class.java)
//
//        // 'jpsTest' does not require the 'tests-jar' artifact
//        project.configurations.create("jpsTest")
//
//        if (project == project.rootProject) {
//            project.tasks.register("pill") {
////                dependsOn(":pill:pill-importer:pill")
//
//                if (System.getProperty("pill.android.tests", "false") == "true") {
//                    TaskUtils.useAndroidSdk(this)
//                    TaskUtils.useAndroidJar(this)
//                }
//            }
//
//            project.tasks.register("unpill") {
//                dependsOn(":pill:pill-importer:unpill")
//            }
//        }
    }
}
