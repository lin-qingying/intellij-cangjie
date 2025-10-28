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
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.service.cangjieProjectService
import org.cangnova.cangjie.project.ui.toolwindow.CjProjectToolWindow
import org.cangnova.cangjie.task.taskQueue

/**
 * 刷新仓颉项目的动作
 *
 * 该动作用于重新加载和刷新所有仓颉项目的结构。
 * 当项目配置文件发生变化或需要重新同步项目状态时使用。
 */
class RefreshProjectAction : CangJieProjectActionBase() {

    init {
        templatePresentation.text = "Refresh Projects"
        templatePresentation.description = "Refresh all CangJie projects"
        templatePresentation.icon = AllIcons.Actions.Refresh
    }

    override fun actionPerformed(e: AnActionEvent) {

        val project: Project = e.project ?: return
        // 保存所有文档，确保文件系统与内存中的内容同步
//        可以立马刷新项目模型
        FileDocumentManager.getInstance().saveAllDocuments()

project.cangjieProjectService.refreshAllProjects()

    }

    override fun updatePresentation(e: AnActionEvent, presentation: Presentation) {
        val project = e.project
        presentation.isEnabled = project != null && project.cangjieProjectService.allProjects.isNotEmpty()
    }
}