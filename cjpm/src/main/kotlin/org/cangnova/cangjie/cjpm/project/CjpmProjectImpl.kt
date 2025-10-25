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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm.config.CjpmTomlParserAdapter
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjWorkspace

/**
 * CJPM 项目实现
 */
class CjpmProjectImpl(
    override val rootDir: VirtualFile,
    override val intellijProject: Project,
    private val manifestFile: VirtualFile
) : CjProject {

    private val config by lazy {
        CjpmTomlParserAdapter.parse(manifestFile)
    }

    override val name: String
        get() = config?.`package`?.name ?: config?.workspace?.name ?: rootDir.name

    override val configFile: VirtualFile
        get() = manifestFile

    override val modules: List<CjModule> by lazy {
        buildModulesList()
    }

    override val workspace: CjWorkspace? by lazy {
        buildWorkspace()
    }

    override val version: String?
        get() = config?.`package`?.version

    override val isValid: Boolean
        get() = config != null

    override fun refresh() {
        // 清除缓存,重新加载配置
        // 这里可以触发模块重新构建等操作
    }

    override fun findModule(name: String): CjModule? {
        return modules.find { it.name == name }
    }

    private fun buildModulesList(): List<CjModule> {
        val config = this.config ?: return emptyList()
        val result = mutableListOf<CjModule>()

        // 如果是单模块项目
        config.`package`?.let { packageConfig ->
            result.add(
                CjpmModuleImpl(
                    name = packageConfig.name,
                    rootDir = rootDir,
                    project = this,
                    packageConfig = packageConfig
                )
            )
        }

        // 如果是工作空间项目
        config.workspace?.let { workspaceConfig ->
            workspaceConfig.members.forEach { memberPath ->
                val memberDir = rootDir.findFileByRelativePath(memberPath)
                val memberManifest = memberDir?.findChild("cjpm.toml")

                if (memberDir != null && memberManifest != null) {
                    val memberConfig = CjpmTomlParserAdapter.parse(memberManifest)
                    memberConfig?.`package`?.let { packageConfig ->
                        result.add(
                            CjpmModuleImpl(
                                name = packageConfig.name,
                                rootDir = memberDir,
                                project = this,
                                packageConfig = packageConfig
                            )
                        )
                    }
                }
            }
        }

        return result
    }

    private fun buildWorkspace(): CjWorkspace? {
        val config = this.config ?: return null

        return if (config.workspace != null) {
            CjpmWorkspaceImpl(
                name = config.workspace.name ?: name,
                rootDir = rootDir,
                projects = modules.map { it as CjpmModuleImpl }
            )
        } else {
            null
        }
    }
}