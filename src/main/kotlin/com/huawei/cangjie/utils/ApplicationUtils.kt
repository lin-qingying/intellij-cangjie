package com.huawei.cangjie.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.impl.CancellationCheck

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
