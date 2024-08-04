package com.linqingying.cangjie.ide.run.cjpm

import com.linqingying.cangjie.ide.run.cjpm.util.executeActionCjpmCommand
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
