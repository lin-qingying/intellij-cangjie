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

package org.cangnova.cangjie.cjpm1.project

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm1.config.CjpmTomlParserAdapter
import org.cangnova.cangjie.cjpm1.model.DependencyConfig
import org.cangnova.cangjie.cjpm1.model.PackageConfig
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

                sourceSets = sourceSets,
                type = TODO()
            )
        )
    }
}

/**
 * CJPM 源码集实现
 */
class CjpmSourceSetImpl(
    override val name: String,

    override val isTest: Boolean
) : CjSourceSet {
    override val sourceRoots: List<VirtualFile>
        get() = TODO("Not yet implemented")
    override val resourceRoots: List<VirtualFile>
        get() = TODO("Not yet implemented")
}

/**
 * CJPM 目标实现
 */
class CjpmTargetImpl(
    override val name: String,
    override val type: CjTargetType,
    override val sourceSets: List<CjSourceSet>
) : CjTarget {


    override val module: CjModule
        get() = TODO("Not yet implemented")
}