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

package org.cangnova.cangjie.cjpm.model

/**
 * CJPM TOML 配置简化模型
 * 这是对现有 cjpm.project.model.toml.CjpmTomlConfig 的简化封装
 */
data class CjpmTomlConfig(
    val `package`: PackageConfig? = null,
    val workspace: WorkspaceConfig? = null,
    val dependencies: Map<String, DependencyConfig> = emptyMap(),
    val testDependencies: Map<String, DependencyConfig> = emptyMap()
)

data class PackageConfig(
    val name: String,
    val version: String,
    val cjcVersion: String,
    val outputType: OutputType,
    val srcDir: String  = "src",
    val targetDir: String  = "target",
    val description: String? = null,
    val compileOption: String? = null,
    val linkOption: String? = null
)

data class WorkspaceConfig(
    val name: String? = null,
    val members: List<String>,
    val buildMembers: List<String>? = null,
    val testMembers: List<String>? = null,
    val targetDir: String? = "target"
)

data class DependencyConfig(
    val version: String? = null,
    val path: String? = null,
    val git: String? = null,
    val branch: String? = null,
    val tag: String? = null,
    val rev: String? = null
)

enum class OutputType {
    EXECUTABLE,
    STATIC,
    DYNAMIC
}
