package com.huawei.cangjie.utils

import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.CancellationCheck
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.ThrowableComputable
import org.jetbrains.annotations.Nls

fun <T> runWithCancellationCheck(block: () -> T): T = CancellationCheck.runWithCancellationCheck(block)
@Suppress("NOTHING_TO_INLINE")
inline fun isDispatchThread(): Boolean = ApplicationManager.getApplication().isDispatchThread
inline fun <T> runAction(runImmediately: Boolean, crossinline action: () -> T): T {
    if (runImmediately) {
        return action()
    }

    var result: T? = null
    ApplicationManager.getApplication().invokeAndWait {
        CommandProcessor.getInstance().runUndoTransparentAction {
            result = ApplicationManager.getApplication().runWriteAction<T> { action() }
        }
    }
    return result!!
}
fun <T> Project.executeCommand(@NlsContexts.Command name: String, groupId: Any? = null, command: () -> T): T {
    @Suppress("UNCHECKED_CAST") var result: T = null as T
    CommandProcessor.getInstance().executeCommand(this, { result = command() }, name, groupId)
    @Suppress("USELESS_CAST")
    return result as T
}
fun Project.executeWriteCommand(@NlsContexts.Command name: String, command: () -> Unit) {
    CommandProcessor.getInstance().executeCommand(this, { runWriteAction(command) }, name, null)
}

fun <T> Project.executeWriteCommand(@NlsContexts.Command name: String, groupId: Any? = null, command: () -> T): T {
    return executeCommand(name, groupId) { runWriteAction(command) }
}
fun <T: Any> underModalProgressOrUnderWriteActionWithNonCancellableProgressInDispatchThread(
    project: Project,
    @Nls progressTitle: String,
    computable: () -> T
): T {
    return if (CommandProcessor.getInstance().currentCommandName != null) {
        lateinit var result: T
        val application = ApplicationManager.getApplication() as ApplicationEx
        application.runWriteActionWithNonCancellableProgressInDispatchThread(progressTitle, project, null) {
            result = computable()
        }
        result
    } else {
        ActionUtil.underModalProgress(project, progressTitle, computable)
    }
}
fun <T> executeInBackgroundWithProgress(project: Project? = null, @NlsContexts.ProgressTitle title: String, block: () -> T): T {
    assert(!ApplicationManager.getApplication().isWriteAccessAllowed) {
        "Rescheduling computation into the background is impossible under the write lock"
    }
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(
        ThrowableComputable { block() }, title, true, project
    )
}
