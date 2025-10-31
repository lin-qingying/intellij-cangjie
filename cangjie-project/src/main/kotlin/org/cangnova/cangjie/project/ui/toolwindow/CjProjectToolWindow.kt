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

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectListener
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.CjProjectsService
import javax.swing.JComponent

/**
 * 仓颉项目工具窗口主类
 *
 * 负责创建和管理工具窗口的工具栏和树形视图
 */
class CjProjectToolWindow(private val project: Project) {

    private val projectTree: CjProjectTree = CjProjectTree(project)

    /**
     * 工具栏
     */
    val toolbar: ActionToolbar = run {
        val actionManager = ActionManager.getInstance()
        actionManager.createActionToolbar(
            CJPROJECT_TOOLBAR_PLACE,
            actionManager.getAction("CangJie.CjProject") as DefaultActionGroup,
            true
        )
    }

    /**
     * 主内容面板
     */
    val content: JComponent = ScrollPaneFactory.createScrollPane(projectTree, true)

    init {
        // 订阅项目变更事件
        with(project.messageBus.connect()) {
            subscribe(CjProjectListener.TOPIC, object : CjProjectListener {
                override fun projectCreated(event: CjProjectEvent) = updateTree(event)
                override fun projectUpdated(event: CjProjectEvent) = updateTree(event)
                override fun projectRemoved(event: CjProjectEvent) = updateTree(event)
                override fun projectConfigChanged(event: CjProjectEvent) = updateTree(event)

                private fun updateTree(event: CjProjectEvent) {
                    // 直接使用事件中的项目对象，避免时序问题
                    invokeLater {
                        projectTree.updateProjects(event.project)
                    }
                }
            })
        }

        // 初始加载项目
        val projectsService = project.getService(CjProjectsService::class.java)
        if (projectsService != null) {
            invokeLater {
                projectTree.updateProjects(projectsService.cjProject )
            }
        }
    }

    companion object {
        /**
         * 用于在 DataContext 中传递选中的仓颉项目
         */
        val SELECTED_CJ_PROJECT: DataKey<CjProject> = DataKey.create("SELECTED_CJ_PROJECT")

        /**
         * 工具栏位置标识
         */
        const val CJPROJECT_TOOLBAR_PLACE: String = "CjProject Toolbar"
    }
}