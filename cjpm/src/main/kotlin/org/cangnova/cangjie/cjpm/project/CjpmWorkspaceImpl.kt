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
import org.cangnova.cangjie.cjpm.project.model.toml.WorkspaceConfig
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjSourceSet
import org.cangnova.cangjie.project.model.CjWorkspace

/**
 * CJPM 工作空间实现
 */
class CjpmWorkspaceImpl(

    override val rootDir: VirtualFile,
    val workspace: WorkspaceConfig,
    override val project: CjProject


    ) : CjWorkspace {
    override val name: String = "Workspace"


    private fun buildModuleList(): List<CjModule> {
        val config = workspace
        var result = mutableListOf<CjModule>()


        // 如果是工作空间项目
        workspace.let { workspaceConfig ->
            workspaceConfig.members.forEach { memberPath ->
                val memberDir = rootDir.findFileByRelativePath(memberPath)
                val memberManifest = memberDir?.findChild("cjpm.toml")

                if (memberDir != null && memberManifest != null) {
                    val memberConfig = CjpmTomlParser.parse(memberManifest)
                    memberConfig?.`package`?.let { packageConfig ->

                        result.add(
                            CjpmModuleImpl(
                                name = packageConfig.name,
                                rootDir = memberDir,
                                project = project,

                                packageConfig = packageConfig
                            )
                        )
                    }
                }
            }
        }

        return result
    }

    private val modulesCache = resettableLazy {
        buildModuleList()
    }

    override val modules get() = modulesCache.value
    override val configFile: VirtualFile?
        get() = rootDir.findChild("cjpm.toml")


    /**
     * 工作空间的源码集
     * 只包含输出目录信息，用于配置主模块的排除目录
     */
    override val sourceSets: List<CjSourceSet> by lazy {
        buildWorkspaceSourceSets()
    }

    private fun buildWorkspaceSourceSets(): List<CjSourceSet> {
        val targetDir = workspace.targetDir ?: "target"
        return listOf(
            CjpmWorkspaceSourceSet(
                rootDir = rootDir,
                targetDir = targetDir
            )
        )
    }

    override fun findModule(name: String): CjModule? {
        return modules.find { it.name == name }
    }
}

/**
 * 工作空间源码集实现
 * 仅提供输出目录信息，其他属性返回空列表
 */
private class CjpmWorkspaceSourceSet(
    private val rootDir: VirtualFile,
    private val targetDir: String
) : CjSourceSet {
    override val name: String = "workspace"
    override val isTest: Boolean = false

    override val sourceRoots: List<VirtualFile> = emptyList()
    override val resourceRoots: List<VirtualFile> = emptyList()

    override val outputDirectory: List<VirtualFile>
        get() {
            val targetDirFile = rootDir.findFileByRelativePath(targetDir)
            return if (targetDirFile != null && targetDirFile.isDirectory) {
                listOf(targetDirFile)
            } else {
                emptyList()
            }
        }
}