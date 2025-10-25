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
import org.cangnova.cangjie.cjpm.config.CjpmTomlParserAdapter
import org.cangnova.cangjie.cjpm.model.DependencyConfig
import org.cangnova.cangjie.cjpm.model.PackageConfig
import org.cangnova.cangjie.dependency.model.CjDependency
import org.cangnova.cangjie.dependency.model.CjDependencyScope
import org.cangnova.cangjie.dependency.model.CjDependencyType
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjSourceSet
import org.cangnova.cangjie.project.model.CjTarget

/**
 * CJPM 模块实现
 */
class CjpmModuleImpl(
    override val name: String,
    override val rootDir: VirtualFile,
    override val project: CjProject,
    val packageConfig: PackageConfig
) : CjModule {

    override val version: String
        get() = packageConfig.version

    override val configFile: VirtualFile?
        get() = rootDir.findChild("cjpm.toml")

    override val sourceSets: List<CjSourceSet> by lazy {
        buildSourceSets()
    }

    override val targets: List<CjTarget> by lazy {
        buildTargets()
    }

    override val dependencies: List<CjDependency> by lazy {
        buildDependencies()
    }

    private fun buildDependencies(): List<CjDependency> {
        val manifestFile = configFile ?: return emptyList()
        val config = CjpmTomlParserAdapter.parse(manifestFile) ?: return emptyList()

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
            version = config.version ?: "latest",
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
                    roots = listOf(srcFile),
                    isTest = false
                )
            )
        } else {
            emptyList()
        }
    }

    private fun buildTargets(): List<CjTarget> {
        return listOf(
            CjpmTargetImpl(
                name = name,
                kind = when (packageConfig.outputType) {
                    org.cangnova.cangjie.cjpm.model.OutputType.EXECUTABLE -> CjTarget.Kind.EXECUTABLE
                    org.cangnova.cangjie.cjpm.model.OutputType.STATIC -> CjTarget.Kind.STATIC_LIB
                    org.cangnova.cangjie.cjpm.model.OutputType.DYNAMIC -> CjTarget.Kind.DYNAMIC_LIB
                },
                sourceSets = sourceSets
            )
        )
    }
}

/**
 * CJPM 源码集实现
 */
class CjpmSourceSetImpl(
    override val name: String,
    override val roots: List<VirtualFile>,
    override val isTest: Boolean
) : CjSourceSet

/**
 * CJPM 目标实现
 */
class CjpmTargetImpl(
    override val name: String,
    override val kind: CjTarget.Kind,
    override val sourceSets: List<CjSourceSet>
) : CjTarget