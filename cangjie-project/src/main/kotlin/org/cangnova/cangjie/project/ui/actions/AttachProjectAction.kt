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

package org.cangnova.cangjie.project.ui.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.service.cangjieProjectService
import org.cangnova.cangjie.project.extension.CjProjectProvider

/**
 * 附加仓颉项目的动作
 *
 * 该动作允许用户选择并导入现有的仓颉项目到当前 IDE 项目中。
 * 用户可以通过文件选择器选择包含仓颉项目配置的目录。
 */
class AttachProjectAction : CangJieProjectActionBase() {

    init {
        templatePresentation.text = "Attach CangJie Project"
        templatePresentation.description = "Attach an existing CangJie project to the current IDE project"
        templatePresentation.icon = AllIcons.General.Add
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        // 创建文件选择器描述符
        val descriptor = FileChooserDescriptor(
            true, // chooseFiles
            false, // chooseFolders
            false, // chooseJars
            false, // chooseJarsAsFiles
            false, // chooseJarContents
            true   // chooseMultiple
        ).withTitle("Select CangJie Project Root Directory")
            .withDescription("Select a directory containing a CangJie project (cjpm.toml or similar)")
            .withHideIgnored(true)

        // 显示文件选择器
        val selectedFiles: Array<VirtualFile> = FileChooser.chooseFiles(descriptor, project, null)

        if (selectedFiles.isEmpty()) return

        // 尝试将选中的文件/目录作为仓颉项目导入
        val projectService = project.cangjieProjectService
        val providers = CjProjectProvider.EP_NAME.extensionList

        for (file in selectedFiles) {
            val targetDir = if (file.isDirectory) file else file.parent ?: continue

            // 尝试使用每个提供者来处理选中的目录
            for (provider in providers) {
                if (provider.canHandle(targetDir)) {
                    val cjProject = provider.createProject(targetDir, project)
                    if (cjProject != null) {
                        projectService.addProject(cjProject)
                        break
                    }
                }
            }
        }
    }

  }