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

package org.cangnova.cangjie.cjpm.config

import org.cangnova.cangjie.cjpm.model.CjpmTomlConfig as SimpleCjpmTomlConfig
import org.cangnova.cangjie.cjpm.model.DependencyConfig as SimpleDependencyConfig
import org.cangnova.cangjie.cjpm.model.OutputType as SimpleOutputType
import org.cangnova.cangjie.cjpm.model.PackageConfig as SimplePackageConfig
import org.cangnova.cangjie.cjpm.model.WorkspaceConfig as SimpleWorkspaceConfig
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlConfig
import org.cangnova.cangjie.cjpm.project.model.toml.DependencyConfig
import org.cangnova.cangjie.cjpm.project.model.toml.OutputType
import org.cangnova.cangjie.cjpm.project.model.toml.PackageConfig
import org.cangnova.cangjie.cjpm.project.model.toml.WorkspaceConfig

/**
 * CJPM 配置转换器
 *
 * 将完整的 CJPM TOML 配置类转换为简化的配置类
 */
object CjpmConfigConverter {

    /**
     * 将完整的配置类转换为简化的配置类
     */
    fun convertToSimpleConfig(fullConfig: CjpmTomlConfig): SimpleCjpmTomlConfig {
        return SimpleCjpmTomlConfig(
            `package` = fullConfig.`package`?.let { convertPackageConfig(it) },
            workspace = fullConfig.workspace?.let { convertWorkspaceConfig(it) },
            dependencies = fullConfig.dependencies.mapValues { convertDependencyConfig(it.value) },
            testDependencies = fullConfig.testDependencies.mapValues { convertDependencyConfig(it.value) }
        )
    }

    private fun convertPackageConfig(full: PackageConfig): SimplePackageConfig {
        return SimplePackageConfig(
            name = full.name,
            version = full.version,
            cjcVersion = full.cjcVersion,
            outputType = convertOutputType(full.outputType),
            srcDir = full.srcDir,
            targetDir = full.targetDir,
            description = full.description,
            compileOption = full.compileOption,
            linkOption = full.linkOption
        )
    }

    private fun convertWorkspaceConfig(full: WorkspaceConfig): SimpleWorkspaceConfig {
        return SimpleWorkspaceConfig(
            name = full.name,
            members = full.members,
            buildMembers = full.buildMembers,
            testMembers = full.testMembers,
            targetDir = full.targetDir
        )
    }

    private fun convertDependencyConfig(full: DependencyConfig): SimpleDependencyConfig {
        return SimpleDependencyConfig(
            version = full.version,
            path = full.path,
            git = full.git,
            branch = full.branch,
            tag = full.tag,
            rev = full.commitId // 简化版本中使用 rev 字段对应完整版本的 commitId
        )
    }

    private fun convertOutputType(full: OutputType): SimpleOutputType {
        return when (full) {
            OutputType.EXECUTABLE -> SimpleOutputType.EXECUTABLE
            OutputType.STATIC -> SimpleOutputType.STATIC
            OutputType.DYNAMIC -> SimpleOutputType.DYNAMIC
        }
    }
}