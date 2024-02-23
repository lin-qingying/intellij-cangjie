package com.huawei.cangjie.cjpm.project.toolwindow

import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.model.guessAndSetupCangJieProject
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

    override fun isApplicable(project: Project): Boolean {
        if (CjpmToolWindow.isRegistered(project)) return false

        val cargoProjects = project.cjpmProjects
        if (!cargoProjects.hasAtLeastOneValidProject
            && cargoProjects.suggestManifests().none()
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


private class CjpmToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, false){


    private val cjpmTab = CjpmToolWindow(project)
    init {
        toolbar = cjpmTab.toolbar.component
        cjpmTab.toolbar.targetComponent = this
        setContent(cjpmTab.content)
    }
}