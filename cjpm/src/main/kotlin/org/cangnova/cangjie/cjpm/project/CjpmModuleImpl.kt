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
import org.cangnova.cangjie.cjpm.config.CjpmConfigConverter
import org.cangnova.cangjie.cjpm.model.DependencyConfig
import org.cangnova.cangjie.cjpm.model.PackageConfig
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlParser
import org.cangnova.cangjie.model.CjDependency
import org.cangnova.cangjie.model.CjDependencyScope
import org.cangnova.cangjie.model.CjDependencyType
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjModuleDependency
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjSourceSet

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
     * 模块依赖列表
     *
     * 从 cjpm.toml 中解析路径依赖(path dependencies)，
     * 将它们转换为模块依赖关系
     */
    override val dependencies: List<CjModuleDependency> by lazy {
        buildModuleDependencies()
    }

    /**
     * 构建模块依赖列表
     *
     * 从 cjpm.toml 配置中提取路径依赖(path dependencies)，
     * 这些依赖通常指向同一工作空间中的其他模块。
     */
    private fun buildModuleDependencies(): List<CjModuleDependency> {
        val manifestFile = configFile ?: return emptyList()
        val fullConfig = CjpmTomlParser.parse(manifestFile) ?: return emptyList()
        val config = CjpmConfigConverter.convertToSimpleConfig(fullConfig)

        val result = mutableListOf<CjModuleDependency>()

        // 解析编译时依赖
        config.dependencies.forEach { (name, depConfig) ->
            // 只处理路径依赖(path dependencies)，这些通常是模块依赖
            if (depConfig.path != null) {
                result.add(
                    CjModuleDependency(
                        moduleName = name,
                        scope = org.cangnova.cangjie.project.model.CjDependencyScope.COMPILE,
                        exported = false
                    )
                )
            }
        }

        // 解析测试时依赖
        config.testDependencies.forEach { (name, depConfig) ->
            if (depConfig.path != null) {
                result.add(
                    CjModuleDependency(
                        moduleName = name,
                        scope = org.cangnova.cangjie.project.model.CjDependencyScope.TEST,
                        exported = false
                    )
                )
            }
        }

        return result
    }



    private fun buildDependencies(): List<CjDependency> {
        val manifestFile = configFile ?: return emptyList()
        val fullConfig = CjpmTomlParser.parse(manifestFile) ?: return emptyList()
        val config = CjpmConfigConverter.convertToSimpleConfig(fullConfig)

        val result = mutableListOf<CjDependency>()

        // Parse compile dependencies
        config.dependencies.forEach { (name, depConfig) ->
            result.add(createDependency(name, depConfig, CjDependencyScope.COMPILE))
        }

        // Parse test dependencies
        config.testDependencies.forEach { (name, depConfig) ->
            result.add(createDependency(name, depConfig, CjDependencyScope.TEST))
        }

        return result
    }

    private fun createDependency(
        name: String,
        config: DependencyConfig,
        scope: CjDependencyScope
    ): CjDependency {
        return CjpmDependency(
            name = name,
            versionString = config.version ?: "latest",
            scope = scope,
            type = when {
                config.path != null -> CjDependencyType.MODULE
                config.git != null -> CjDependencyType.LIBRARY
                else -> CjDependencyType.LIBRARY
            },
            path = config.path,
            git = config.git,
            branch = config.branch,
            tag = config.tag,
            rev = config.rev
        )
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

 