package com.huawei.cangjie.idea.run.cjpm

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class RunMainAction : DumbAwareAction() {
    companion object {
        const val ID = "CjpmRunMain"
    }
    override fun actionPerformed(e: AnActionEvent) {
        executeActionCjpmCommand(e, "run-Main", "run")
    }
}
