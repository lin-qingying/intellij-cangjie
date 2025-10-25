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

package org.cangnova.cangjie.project.ui.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.SimpleToolWindowPanel
import com.intellij.ui.content.ContentFactory
import org.cangnova.cangjie.project.service.CjProjectsService

/**
 * 仓颉项目工具窗口工厂
 *
 * 创建显示项目模型（Workspace/Project/Module）的工具窗口
 */
class CjProjectToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolwindowPanel = CjProjectToolWindowPanel(project)
        val tab = ContentFactory.getInstance()
            .createContent(toolwindowPanel, "", false)
        toolWindow.contentManager.addContent(tab)
    }

    override fun isApplicable(project: Project): Boolean {
        // 仅当存在仓颉项目时显示工具窗口
        val service = project.getService(CjProjectsService::class.java) ?: return false
        return service.allProjects.isNotEmpty()
    }
}

/**
 * 工具窗口面板
 */
private class CjProjectToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, false) {

    private val cjProjectWindow = CjProjectToolWindow(project)

    init {
        toolbar = cjProjectWindow.toolbar.component
        cjProjectWindow.toolbar.targetComponent = this
        setContent(cjProjectWindow.content)
    }
}