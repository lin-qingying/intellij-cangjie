package com.linqingying.lsp.impl.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.codeWithMe.ClientId
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager

class CurrentActionListener : AnActionListener {




    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        val service = ApplicationManager.getApplication().getService(CurrentActionHolder::class.java)
            ?: throw RuntimeException("Cannot find service ${CurrentActionHolder::class.java.name} (classloader=${CurrentActionHolder::class.java.classLoader}, client=${ClientId.currentOrNull})")

        service.runningGoToDeclarationAction = false
    }

    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {

        if (action is GotoDeclarationAction) {
            val service = ApplicationManager.getApplication().getService(CurrentActionHolder::class.java)
                ?: throw RuntimeException("Cannot find service ${CurrentActionHolder::class.java.name} (classloader=${CurrentActionHolder::class.java.classLoader}, client=${ClientId.Companion.currentOrNull})")

            service.runningGoToDeclarationAction = true
        }
    }

}
