/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectListener
import org.cangnova.cangjie.project.service.CjProjectsService

/**
 * 仓颉项目工具窗口工厂
 *
 * 创建显示项目模型（Workspace/Project/Module）的工具窗口
 */
internal class CjProjectToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {
        const val TOOL_WINDOW_ID = "CangJie"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolwindowPanel = CjProjectToolWindowPanel(project)
        val tab = ContentFactory.getInstance()
            .createContent(toolwindowPanel, "", false)
        toolWindow.contentManager.addContent(tab)

        // 订阅项目事件，动态更新工具窗口可见性
        // 只要有仓颉项目就显示，不管项目是否有效
        project.messageBus.connect(toolWindow.disposable)
            .subscribe(CjProjectListener.TOPIC, object : CjProjectListener {
                override fun projectCreated(event: CjProjectEvent) {
                    updateToolWindowAvailability(project, true)
                }

                override fun projectOpened(event: CjProjectEvent) {
                    updateToolWindowAvailability(project, true)
                }

                override fun projectUpdated(event: CjProjectEvent) {
                    // 项目更新时保持可见
                    updateToolWindowAvailability(project, true)
                }

                override fun projectRemoved(event: CjProjectEvent) {
                    // 项目移除时隐藏
                    updateToolWindowAvailability(project, false)
                }

                override fun projectSynced(event: CjProjectEvent) {
                    // 同步完成后保持可见
                    updateToolWindowAvailability(project, true)
                }
            })
    }

    /**
     * 更新工具窗口可用性
     *
     * 必须在 EDT（Event Dispatch Thread）上调用
     */
    private fun updateToolWindowAvailability(project: Project, available: Boolean) {
        invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)
            toolWindow?.setAvailable(available)
        }
    }

    override suspend fun isApplicableAsync(project: Project): Boolean {
        // 始终返回 true，让工具窗口注册
        // 实际可见性通过 shouldBeAvailable 控制
        return true
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        // 初始可见性：只要有仓颉项目就显示，不管是否有效
        val service = project.getService(CjProjectsService::class.java)
        return service?.cjProject != null
    }

    @Deprecated("Use isApplicableAsync")
    override fun isApplicable(project: Project): Boolean {
        return true
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

    override fun uiDataSnapshot(sink: com.intellij.openapi.actionSystem.DataSink) {
        super.uiDataSnapshot(sink)
        sink[PlatformDataKeys.TREE_EXPANDER] = cjProjectWindow.treeExpander
    }
}