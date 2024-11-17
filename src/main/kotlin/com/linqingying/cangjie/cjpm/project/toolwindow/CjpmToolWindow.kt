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

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.model.CjpmProject
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.ToolWindowManagerImpl
import com.intellij.ui.ColorUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import javax.swing.JComponent
import javax.swing.JEditorPane

val Project.hasCjpmProject: Boolean get() = cjpmProjects.allProjects.isNotEmpty()

class CjpmToolWindow(
    private val project: Project
) {
    val toolbar: ActionToolbar = run {
        val actionManager = ActionManager.getInstance()
        actionManager.createActionToolbar(CJPM_TOOLBAR_PLACE, actionManager.getAction("CangJie.Cjpm") as DefaultActionGroup, true)
    }

    val note = JEditorPane("text/html", html("")).apply {
        background = UIUtil.getTreeBackground()
        isEditable = false
    }

    private val projectTree = CjpmProjectsTree()


//    private val projectStructure = CjpmProjectTreeStructure(projectTree, project)

//    val treeExpander: TreeExpander = object : DefaultTreeExpander(projectTree) {
//        override fun isCollapseAllVisible(): Boolean = project.hasCjpmProject
//        override fun isExpandAllVisible(): Boolean = project.hasCjpmProject
//    }

//    val selectedProject: CjpmProject get() = projectTree.selectedProject

    val content: JComponent = ScrollPaneFactory.createScrollPane(projectTree, 0)

//    init {
//        with(project.messageBus.connect()) {
//            subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, CjpmProjectsService.CjpmProjectsListener { _, projects ->
//                invokeLater {
//                    projectStructure.updateCjpmProjects(projects.toList())
//                }
//            })
//        }
//
//        invokeLater {
//            projectStructure.updateCjpmProjects(project.cjpmProjects.allProjects.toList())
//        }
//    }

    @Nls
    private fun html(body: String): String = CangJieBundle.message("html.head.0.style.body.background.1.text.align.center.style.head.body.2.body.html", UIUtil.getCssFontDeclaration(
        UIUtil.getLabelFont()), ColorUtil.toHex(UIUtil.getTreeBackground()), body)

    companion object {
        private val LOG: Logger = logger<CjpmToolWindow>()

        @JvmStatic
        val SELECTED_CJPM_PROJECT: DataKey<CjpmProject> = DataKey.create("SELECTED_CJPM_PROJECT")

        const val CJPM_TOOLBAR_PLACE: String = "Cjpm Toolbar"

        private const val ID: String = "Cjpm"

        fun initializeToolWindow(project: Project) {
            try {
                @Suppress("UnstableApiUsage")
                val manager = ToolWindowManager.getInstance(project) as? ToolWindowManagerImpl ?: return
                val bean = ToolWindowEP.EP_NAME.extensionList.find { it.id == ID }
                if (bean != null) {
                    @Suppress("DEPRECATION", "UnstableApiUsage")
                    manager.initToolWindow(bean)
                }
            } catch (e: Exception) {
                LOG.error("Unable to initialize $ID tool window", e)
            }
        }

        fun isRegistered(project: Project): Boolean {
            val manager = ToolWindowManager.getInstance(project)
            return manager.getToolWindow(ID) != null
        }
    }
}
