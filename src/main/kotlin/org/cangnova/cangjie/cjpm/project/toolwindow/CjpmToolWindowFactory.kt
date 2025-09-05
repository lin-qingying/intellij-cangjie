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

package org.cangnova.cangjie.cjpm.project.toolwindow

import com.intellij.openapi.actionSystem.PlatformDataKeys
import org.cangnova.cangjie.cjpm.project.model.cjpmProjects
import org.cangnova.cangjie.cjpm.project.model.guessAndSetupCangJieProject
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class CjpmToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        guessAndSetupCangJieProject(project)
        val toolwindowPanel = CjpmToolWindowPanel(project)
        val tab = ContentFactory.getInstance()
            .createContent(toolwindowPanel, "", false)
        toolWindow.contentManager.addContent(tab)
    }

    private val lock: Any = Any()

    companion object {
        private val CJPM_TOOL_WINDOW_APPLICABLE: Key<Boolean> = Key.create("CJPM_TOOL_WINDOW_APPLICABLE")
    }

    @Deprecated("Use isApplicableAsync")
    override fun isApplicable(project: Project): Boolean {
        if (CjpmToolWindow.isRegistered(project)) return false

        val cjpmProjects = project.cjpmProjects
        if (!cjpmProjects.hasAtLeastOneValidProject
            && cjpmProjects.suggestManifests().none()
        ) return false

        synchronized(lock) {
            val res = project.getUserData(CJPM_TOOL_WINDOW_APPLICABLE) ?: true
            if (res) {
                project.putUserData(CJPM_TOOL_WINDOW_APPLICABLE, false)
            }
            return res
        }
    }

}


private class CjpmToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, false) {


    private val cjpmTab = CjpmToolWindow(project)

    init {
        toolbar = cjpmTab.toolbar.component
        cjpmTab.toolbar.targetComponent = this
        setContent(cjpmTab.content)
    }

    @Deprecated("Migrate to [uiDataSnapshot] ASAP")
    override fun getData(dataId: String): Any? =
        when {
            CjpmToolWindow.SELECTED_CJPM_PROJECT.`is`(dataId) -> cjpmTab.selectedProject
            PlatformDataKeys.TREE_EXPANDER.`is`(dataId) -> cjpmTab.treeExpander
            else -> super.getData(dataId)
        }
}
