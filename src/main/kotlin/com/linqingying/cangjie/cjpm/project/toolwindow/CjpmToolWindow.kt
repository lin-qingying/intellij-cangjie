/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.cjpm.project.toolwindow

import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.model.CjpmProject
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.ToolWindowManagerImpl
import com.intellij.ui.ColorUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import com.linqingying.cangjie.cjpm.project.model.CjpmProjectsService
import com.linqingying.cangjie.cjpm.project.model.CjpmProjectsService.CjpmProjectsListener
import org.jetbrains.annotations.Nls
import javax.swing.JComponent
import javax.swing.JEditorPane

import com.intellij.openapi.wm.RegisterToolWindowTask
import com.linqingying.cangjie.icon.CangJieIcons
import com.linqingying.cangjie.ide.project.tools.projectWizard.CangJieUiBundle

// 获取项目是否包含Cjpm项目的属性
val Project.hasCjpmProject: Boolean get() = cjpmProjects.allProjects.isNotEmpty()

/**
 * Cjpm工具窗口类
 * 提供Cjpm相关的工具窗口功能，包括项目树、操作工具栏等
 *
 * @param project 当前的项目实例
 */
class CjpmToolWindow(
    private val project: Project
) {
    // 初始化并创建操作工具栏
    val toolbar: ActionToolbar = run {
        val actionManager = ActionManager.getInstance()
        actionManager.createActionToolbar(
            CJPM_TOOLBAR_PLACE,
            actionManager.getAction("CangJie.Cjpm") as DefaultActionGroup,
            true
        )
    }

    // 初始化Cjpm项目树组件
    private val projectTree = CjpmProjectsTree()
    private val projectStructure = CjpmProjectTreeStructure(projectTree, project)
    val selectedProject: CjpmProject? get() = projectTree.selectedProject
    val treeExpander: TreeExpander = object : DefaultTreeExpander(projectTree) {
        override fun isCollapseAllVisible(): Boolean = project.hasCjpmProject
        override fun isExpandAllVisible(): Boolean = project.hasCjpmProject
    }

    // 初始化并配置只读的HTML内容显示组件
    val note = JEditorPane("text/html", html("")).apply {
        background = UIUtil.getTreeBackground()
        isEditable = false
    }

    // 创建并配置工具窗口的内容区域，使用滚动面板包裹项目树组件
    val content: JComponent = ScrollPaneFactory.createScrollPane(projectTree, 0)

    init {
        with(project.messageBus.connect()) {
            subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, CjpmProjectsListener { _, projects ->
                invokeLater {
                    projectStructure.updateCjpmProjects(projects.toList())
                }
            })
        }

        invokeLater {
            projectStructure.updateCjpmProjects(project.cjpmProjects.allProjects.toList())
        }

        val manager = ToolWindowManager.getInstance(project)
        val toolWindow = manager.getToolWindow(ID) ?: run {
            manager.registerToolWindow(
                RegisterToolWindowTask(
                    id = ID,
                    icon = CangJieIcons.CANGJIE,
                    contentFactory = CjpmToolWindowFactory(),
                    canCloseContent = true,
//                    stripeTitle = { CangJieBundle.message("toolwindow.stripe.Cjpm") }
                )
            )
        }
    }

    /**
     * 生成HTML内容的方法
     * 用于生成要在工具窗口中显示的HTML文本
     *
     * @param body HTML的body内容
     * @return 完整的HTML字符串
     */
    @Nls
    private fun html(body: String): String = CangJieBundle.message(
        "html.head.0.style.body.background.1.text.align.center.style.head.body.2.body.html",
        UIUtil.getCssFontDeclaration(
            UIUtil.getLabelFont()
        ),
        ColorUtil.toHex(UIUtil.getTreeBackground()),
        body
    )

    // 伴生对象，包含日志记录器、数据键和工具窗口相关的常量及静态方法
    companion object {
        // 日志记录器
        private val LOG: Logger = logger<CjpmToolWindow>()

        // 选中的Cjpm项目的数据键
        @JvmStatic
        val SELECTED_CJPM_PROJECT: DataKey<CjpmProject> = DataKey.create("SELECTED_CJPM_PROJECT")

        // 工具窗口的位置常量
        const val CJPM_TOOLBAR_PLACE: String = "Cjpm Toolbar"

        // 工具窗口的ID常量
        private const val ID: String = "Cjpm"

        /**
         * 初始化工具窗口的方法
         * 尝试初始化Cjpm工具窗口，如果出现异常则记录错误日志
         *
         * @param project 当前的项目实例
         */
        fun initializeToolWindow(project: Project) {
//            try {
//                val manager = ToolWindowManager.getInstance(project)
//                if (!isRegistered(project)) {
//                    manager.registerToolWindow(RegisterToolWindowTask(
//                        id = ID,
//                        icon = CangJieIcons.CANGJIE,
//                        contentFactory = CjpmToolWindowFactory(),
//                        canCloseContent = true,
////                        stripeTitle = { CangJieBundle.message("toolwindow.stripe.Cjpm") }
//                    ))
//                }
//            } catch (e: Exception) {
//                LOG.error("Unable to initialize $ID tool window", e)
//            }
        }

        /**
         * 检查工具窗口是否已注册的方法
         *
         * @param project 当前的项目实例
         * @return 如果工具窗口已注册则返回true，否则返回false
         */
        fun isRegistered(project: Project): Boolean {
            val manager = ToolWindowManager.getInstance(project)
            return manager.getToolWindow(ID) != null
        }
    }
}
