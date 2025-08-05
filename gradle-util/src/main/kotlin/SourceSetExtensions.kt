/*
 * Copyright 2025 LinQingYing. and contributors.
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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.ideaExt.idea

/**
 * Project 扩展函数，提供 sourceSets DSL
 */
inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) = SourceSetsBuilder(this).body()

/**
 * SourceSet 扩展函数，设置源目录为空
 */
fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

/**
 * SourceSet 扩展属性，提供项目默认配置
 */
val SourceSet.projectDefault: Project.() -> Unit
    get() = {
        when (val name = this@projectDefault.name) {
            "main" -> {
                java.srcDirs("src")
                this@projectDefault.resources.srcDir("resources")
            }
            "test" -> {
                java.srcDirs("test", "tests")
                this@projectDefault.resources.srcDir("testResources")
            }
            "testFixtures" -> {
                java.srcDirs("testFixtures")
                this@projectDefault.resources.srcDir("testFixturesResources")
            }
            else -> error("Unknown source set $name")
        }
    }

/**
 * SourceSet 扩展属性，提供生成目录配置
 */
val SourceSet.generatedDir: Project.() -> Unit
    get() = {
        generatedDir(this, "gen")
    }

/**
 * SourceSet 扩展属性，提供测试生成目录配置
 */
val SourceSet.generatedTestDir: Project.() -> Unit
    get() = {
        generatedDir(this, "tests-gen")
    }

/**
 * SourceSet 私有扩展函数，配置生成目录
 */
private fun SourceSet.generatedDir(project: Project, dirName: String) {
    val generationRoot = project.projectDir.resolve(dirName)
    java.srcDir(generationRoot.name)

    project.apply(plugin = "idea")
    project.idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

/**
 * Project 扩展属性，获取 SourceSetContainer
 */
val Project.sourceSets: SourceSetContainer
    get() = javaPluginExtension().sourceSets

/**
 * Project 扩展属性，获取主 SourceSet
 */
val Project.mainSourceSet: SourceSet
    get() = javaPluginExtension().mainSourceSet

/**
 * Project 扩展属性，获取测试 SourceSet
 */
val Project.testSourceSet: SourceSet
    get() = javaPluginExtension().testSourceSet

/**
 * JavaPluginExtension 扩展属性，获取主 SourceSet
 */
val JavaPluginExtension.mainSourceSet: SourceSet
    get() = sourceSets.getByName("main")

/**
 * JavaPluginExtension 扩展属性，获取测试 SourceSet
 */
val JavaPluginExtension.testSourceSet: SourceSet
    get() = sourceSets.getByName("test") 