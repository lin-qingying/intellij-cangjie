package com.linqingying.lsp.impl.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.codeWithMe.ClientId
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl
import com.intellij.openapi.application.ApplicationManager

private class CurrentActionListener : AnActionListener {
    override fun afterActionPerformed(
        action: AnAction,
        event: AnActionEvent,
        result: AnActionResult
    ) {
        val service = ApplicationManager.getApplication().getService(CurrentActionHolder::class.java)
            ?: throw RuntimeException("Cannot find service ${CurrentActionHolder::class.java.name} (classloader=${CurrentActionHolder::class.java.classLoader}, client=${ClientId.currentOrNull})")

        service.runningGoToDeclarationAction = false
    }

    override fun beforeActionPerformed(
        action: AnAction,
        event: AnActionEvent
    ) {

        if (action !is GotoDeclarationAction) {
            if (action !is OverridingAction) return

            val actionManager = ActionManager.getInstance() as ActionManagerImpl
            if (actionManager.getBaseAction(action as OverridingAction) !is GotoDeclarationAction) return
        }

        val service = ApplicationManager.getApplication().getService(CurrentActionHolder::class.java)
            ?: throw RuntimeException("Cannot find service ${CurrentActionHolder::class.java.name} (classloader=${CurrentActionHolder::class.java.classLoader}, client=${ClientId.currentOrNull})")

        service.runningGoToDeclarationAction = true
    }
}

