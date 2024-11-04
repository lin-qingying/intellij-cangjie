package com.linqingying.cangjie.cjpm.runconfig.command

import com.linqingying.cangjie.cjpm.project.toolwindow.hasCjpmProject
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction


abstract class RunCjpmCommandActionBase : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        val hasCargoProject = e.project?.hasCjpmProject == true
        e.presentation.isEnabledAndVisible = hasCargoProject
    }
}
