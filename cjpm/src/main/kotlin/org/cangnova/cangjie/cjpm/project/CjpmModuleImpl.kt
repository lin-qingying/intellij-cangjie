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
import org.cangnova.cangjie.cjpm.config.CjpmConfigConverter
import org.cangnova.cangjie.cjpm.model.DependencyConfig
import org.cangnova.cangjie.cjpm.model.PackageConfig
import org.cangnova.cangjie.dependency.model.CjDependency
import org.cangnova.cangjie.dependency.model.CjDependencyScope
import org.cangnova.cangjie.dependency.model.CjDependencyType
import org.cangnova.cangjie.project.model.*

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

    override val targets: List<CjTarget> by lazy {
        buildTargets()
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
        val srcDir = packageConfig.srcDir ?: "src"
        val srcFile = rootDir.findFileByRelativePath(srcDir)

        return if (srcFile != null) {
            listOf(
                CjpmSourceSetImpl(
                    name = "main",
                    rootDir = rootDir,
                    srcDir = srcDir,
                    isTest = false
                )
            )
        } else {
            emptyList()
        }
    }

    private fun buildTargets(): List<CjTarget> {
        val targetType = when (packageConfig.outputType) {
            org.cangnova.cangjie.cjpm.model.OutputType.EXECUTABLE -> CjTargetType.EXECUTABLE
            org.cangnova.cangjie.cjpm.model.OutputType.STATIC -> CjTargetType.STATIC_LIBRARY
            org.cangnova.cangjie.cjpm.model.OutputType.DYNAMIC -> CjTargetType.DYNAMIC_LIBRARY
            null -> CjTargetType.EXECUTABLE // 默认为可执行文件
        }

        return listOf(
            CjpmTargetImpl(
                name = name,
                sourceSets = sourceSets,
                type = targetType,
                module = this
            )
        )
    }
}

/**
 * CJPM 源码集实现
 */
class CjpmSourceSetImpl(
    override val name: String,
    private val rootDir: VirtualFile,
    private val srcDir: String,
    override val isTest: Boolean
) : CjSourceSet {

    override val sourceRoots: List<VirtualFile>
        get() {
            val srcFile = rootDir.findFileByRelativePath(srcDir)
            return if (srcFile != null && srcFile.isDirectory) {
                listOf(srcFile)
            } else {
                emptyList()
            }
        }

    override val resourceRoots: List<VirtualFile>
        get() {
            val resourcesDir = rootDir.findFileByRelativePath("resources")
            return if (resourcesDir != null && resourcesDir.isDirectory) {
                listOf(resourcesDir)
            } else {
                emptyList()
            }
        }
}

/**
 * CJPM 目标实现
 */
class CjpmTargetImpl(
    override val name: String,
    override val type: CjTargetType,
    override val sourceSets: List<CjSourceSet>,
    override val module: CjModule
) : CjTarget {

    override val outputDirectory: VirtualFile?
        get() {
            val targetDir = "target"
            val buildDir = module.rootDir.findFileByRelativePath(targetDir)
            return if (buildDir != null && buildDir.isDirectory) {
                buildDir
            } else {
                null
            }
        }
}