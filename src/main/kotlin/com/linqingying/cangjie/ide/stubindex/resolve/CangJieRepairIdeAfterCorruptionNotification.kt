package com.linqingying.cangjie.ide.stubindex.resolve

import com.linqingying.cangjie.analyzer.CangJieBaseAnalysisBundle
import com.intellij.ide.actions.cache.ProjectRecoveryScope
import com.intellij.ide.actions.cache.Saul
import com.intellij.idea.ActionsBundle
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import java.util.concurrent.atomic.AtomicBoolean



@OptIn(IntellijInternalApi::class)
internal class CangJieRepairIdeAfterCorruptionNotification(private val project: Project) : CangJieCorruptedIndexListener {
    private val pendingNotificationFlag = AtomicBoolean(false)

    override fun corruptionDetected() {
        if (pendingNotificationFlag.get()) return

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Recover CangJie Indices")
            .createNotification(CangJieBaseAnalysisBundle.message("cangjie.indices.corrupted"), NotificationType.ERROR)
            .setSuggestionType(true)
            .setImportantSuggestion(true)
            .addAction(
                NotificationAction.createSimpleExpiring(
                    ActionsBundle.message("action.CallSaul.text"),
                    CangJieRepairIdeAction(project)
                )
            )
            .whenExpired {
                pendingNotificationFlag.set(false)
            }

        if (pendingNotificationFlag.compareAndSet(/* expectedValue = */ false, /* newValue = */ true)) {
            notification.notify(project)
        }
    }
}

private class CangJieRepairIdeAction(private val project: Project) : Runnable {
    override fun run() {
        service<Saul>().sortThingsOut(ProjectRecoveryScope(project))
    }
}
