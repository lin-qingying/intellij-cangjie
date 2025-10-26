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

package org.cangnova.cangjie.cjpm1.wizard

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.service.CjProjectsService

import org.cangnova.cangjie.project.wizard.CjModuleBuilder
import org.cangnova.cangjie.project.wizard.ConfigurationData
import org.cangnova.cangjie.result.CjResult
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig

/**
 * CJPM 项目模块构建器
 *
 * 使用 cjpm 工具创建新项目
 */
class CjpmModuleBuilder : CjModuleBuilder() {

    private val log = logger<CjpmModuleBuilder>()

    /**
     * CJPM 项目配置数据
     */
    override var configurationData: ConfigurationData? = null

    override fun createProjectStructure(
        rootDir: VirtualFile,
        model: ModifiableRootModel
    ): Boolean {
        val config = configurationData ?: return false
        val toolchain = config.settings.toolchain ?: return false

        // 检查是否已存在 cjpm.toml
        if (rootDir.findChild("cjpm.toml") != null) {
            log.info("cjpm.toml already exists, skipping project creation")
            return true
        }

        return try {
            val project = model.project
            val projectName = project.name
                .replace(' ', '_')
                .replace("-", "_")
                .replace(".", "_")


            val projectType = config.projectType

            // 调用 cjpm init 创建项目
            val result = CjProjectsService.getInstance(project).createProject(
                toolchain.id,
                directory = rootDir,
                owner = model.module,
                projectType = projectType

            )

            when (result) {
                is CjResult.Ok -> {
                    log.info("CJPM project created successfully: $projectName")

                    // 设置项目工具链

                    CjProjectSdkConfig.getInstance(project).setProjectSdkId(toolchain.id)


                    // 刷新文件系统
                    rootDir.refresh(false, true)
                    true
                }

                is CjResult.Err -> {
                    log.error("Failed to create CJPM project", result.err)
                    throw ConfigurationException(result.err.message)
                }
            }
        } catch (e: Exception) {
            log.error("Error creating CJPM project", e)
            false
        }
    }


}