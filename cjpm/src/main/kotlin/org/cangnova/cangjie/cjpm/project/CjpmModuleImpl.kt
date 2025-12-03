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

package org.cangnova.cangjie.cjpm.project

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlParser
import org.cangnova.cangjie.cjpm.project.model.toml.DependencyConfig
import org.cangnova.cangjie.cjpm.project.model.toml.PackageConfig
import org.cangnova.cangjie.model.CjDependency
import org.cangnova.cangjie.model.CjDependencyScope
import org.cangnova.cangjie.model.CjVersion
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjSourceSet
import org.cangnova.cangjie.project.model.cjSdk

/**
 * CJPM 模块实现
 */
class CjpmModuleImpl(
    override val name: String,
    override val rootDir: VirtualFile,
    override val project: CjProject,

    val packageConfig: PackageConfig
) : CjModule {


    override val configFile: VirtualFile?
        get() = rootDir.findChild("cjpm.toml")

    override val sourceSets: List<CjSourceSet> by lazy {
        buildSourceSets()
    }

    /**
     * 所有依赖列表
     *
     * 从 cjpm.toml 中解析所有依赖，包括：
     * - 外部库依赖 (Library)
     * - 路径依赖/模块依赖 (Path)
     * - Git 依赖 (Git)
     */
    override val allDependencies: List<CjDependency> by lazy {
        buildAllDependencies()
    }


    /**
     * 构建所有依赖列表
     *
     * 从 cjpm.toml 配置中解析所有类型的依赖
     */
    private fun buildAllDependencies(): List<CjDependency> {
        val manifestFile = configFile ?: return emptyList()
        val config = CjpmTomlParser.parse(manifestFile) ?: return emptyList()

        val result = mutableListOf<CjDependency>()

        // 解析编译时依赖
        config.dependencies.forEach { (name, depConfig) ->
            result.add(createDependencyFromConfig(name, depConfig, CjDependencyScope.COMPILE))
        }

        // 解析测试时依赖
        config.testDependencies.forEach { (name, depConfig) ->
            result.add(createDependencyFromConfig(name, depConfig, CjDependencyScope.TEST))
        }


//        增加stdlib
        project.intellijProject.cjSdk?.let { sdk ->
            result.add(
                CjDependency.Stdlib(

                    version = CjVersion(sdk.version.toString()),
                    scope = CjDependencyScope.COMPILE
                )
            )

        }

        return result
    }

    /**
     * 从配置创建依赖对象（新实现，使用 sealed class）
     */
    private fun createDependencyFromConfig(
        name: String,
        config: DependencyConfig,
        scope: CjDependencyScope
    ): CjDependency {
        val version = CjVersion(config.version)

        return when {
            // 路径依赖
            config.path != null -> CjDependency.Path(
                name = name,
                path = config.path,
                version = version,
                scope = scope
            )

            // Git 依赖
            config.git != null -> CjDependency.Git(
                name = name,
                url = config.git,
                branch = config.branch,
                tag = config.tag,
                rev = config.rev,
                version = version,
                scope = scope
            )

            // 库依赖（默认）
            else -> CjDependency.Library(
                name = name,
                version = version,
                scope = scope
            )
        }
    }


    private fun buildSourceSets(): List<CjSourceSet> {
        val srcDir = packageConfig.srcDir
        val targetDir = packageConfig.targetDir
        val srcFile = rootDir.findFileByRelativePath(srcDir)

        return if (srcFile != null) {
            listOf(
                CjpmSourceSetImpl(
                    name = "main",
                    rootDir = rootDir,
                    srcDir = srcDir,
                    targetDir = targetDir,
                    isTest = false
                )
            )
        } else {
            emptyList()
        }
    }


}

/**
 * CJPM 源码集实现
 */
class CjpmSourceSetImpl(
    override val name: String,
    private val rootDir: VirtualFile,
    private val srcDir: String,
    private val targetDir: String,
    override val isTest: Boolean
) : CjSourceSet {

    override val sourceRoots: List<VirtualFile>
        get() {
            if (!rootDir.isValid) return emptyList()
            val srcFile = rootDir.findFileByRelativePath(srcDir)
            return if (srcFile != null && srcFile.isValid && srcFile.isDirectory) {
                listOf(srcFile)
            } else {
                emptyList()
            }
        }

    override val resourceRoots: List<VirtualFile>
        get() {
            if (!rootDir.isValid) return emptyList()
            val resourcesDir = rootDir.findFileByRelativePath("resources")
            return if (resourcesDir != null && resourcesDir.isValid && resourcesDir.isDirectory) {
                listOf(resourcesDir)
            } else {
                emptyList()
            }
        }

    override val outputDirectory: List<VirtualFile>
        get() {
            if (!rootDir.isValid) return emptyList()
            val targetDirFile = rootDir.findFileByRelativePath(targetDir)
            return if (targetDirFile != null && targetDirFile.isValid && targetDirFile.isDirectory) {
                listOf(targetDirFile)
            } else {
                emptyList()
            }
        }
}

 