package com.huawei.cangjie.cjpm.runconfig.command

import com.huawei.cangjie.ide.actions.runAnything.cjpm.CjpmRunAnythingProvider.Companion.HELP_COMMAND
import com.intellij.ide.actions.runAnything.RunAnythingManager
import com.intellij.openapi.actionSystem.AnActionEvent


class RunCjpmCommandAction : RunCjpmCommandActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val runAnythingManager = RunAnythingManager.getInstance(project)
        runAnythingManager.show("$HELP_COMMAND ", false, e)
    }
}
