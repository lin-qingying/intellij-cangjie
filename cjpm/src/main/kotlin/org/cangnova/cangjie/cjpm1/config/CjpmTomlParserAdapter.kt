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

package org.cangnova.cangjie.cjpm1.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm1.model.*
import java.io.IOException

/**
 * CJPM TOML 配置解析器适配器
 *
 * 将现有的 cjpm.toml 解析逻辑适配到新的模块架构
 */
object CjpmTomlParserAdapter {

    private val mapper = ObjectMapper(TomlFactory()).apply {
        registerModule(KotlinModule.Builder().build())
    }

    /**
     * 解析 cjpm.toml 文件
     */
    fun parse(file: VirtualFile): CjpmTomlConfig? {
        return try {
            val content = String(file.contentsToByteArray())
            val rawConfig = mapper.readValue(content, RawTomlConfig::class.java)
            convertToConfig(rawConfig)
        } catch (e: IOException) {
            null
        }
    }

    private fun convertToConfig(raw: RawTomlConfig): CjpmTomlConfig {
        return CjpmTomlConfig(
            `package` = raw.`package`?.let { convertPackageConfig(it) },
            workspace = raw.workspace?.let { convertWorkspaceConfig(it) },
            dependencies = raw.dependencies ?: emptyMap(),
            testDependencies = raw.testDependencies ?: emptyMap()
        )
    }

    private fun convertPackageConfig(raw: RawPackageConfig): PackageConfig {
        return PackageConfig(
            name = raw.name,
            version = raw.version,
            cjcVersion = raw.cjcVersion,
            outputType = raw.outputType ?: OutputType.EXECUTABLE,
            srcDir = raw.srcDir,
            targetDir = raw.targetDir,
            description = raw.description,
            compileOption = raw.compileOption,
            linkOption = raw.linkOption
        )
    }

    private fun convertWorkspaceConfig(raw: RawWorkspaceConfig): WorkspaceConfig {
        return WorkspaceConfig(
            name = raw.name,
            members = raw.members,
            buildMembers = raw.buildMembers,
            testMembers = raw.testMembers,
            targetDir = raw.targetDir
        )
    }

    // 原始配置数据类（用于Jackson反序列化）
    private data class RawTomlConfig(
        val `package`: RawPackageConfig? = null,
        val workspace: RawWorkspaceConfig? = null,
        val dependencies: Map<String, DependencyConfig>? = null,
        val testDependencies: Map<String, DependencyConfig>? = null
    )

    private data class RawPackageConfig(
        val name: String,
        val version: String,
        val cjcVersion: String,
        val outputType: OutputType? = null,
        val srcDir: String? = null,
        val targetDir: String? = null,
        val description: String? = null,
        val compileOption: String? = null,
        val linkOption: String? = null
    )

    private data class RawWorkspaceConfig(
        val name: String? = null,
        val members: List<String>,
        val buildMembers: List<String>? = null,
        val testMembers: List<String>? = null,
        val targetDir: String? = null
    )
}