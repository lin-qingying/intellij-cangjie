package cn.cangnova.cangjie.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode
val isDispatchThread: Boolean get() = ApplicationManager.getApplication().isDispatchThread
val isInternal: Boolean get() = ApplicationManager.getApplication().isInternal


fun <T> invokeAndWaitIfNeeded(modalityState: ModalityState? = null, runnable: () -> T): T {
    val app = ApplicationManager.getApplication()
    if (app.isDispatchThread) {
        return runnable()
    } else {
        var resultRef: T? = null
        app.invokeAndWait({ resultRef = runnable() }, modalityState ?: ModalityState.defaultModalityState())
        @Suppress("UNCHECKED_CAST")
        return resultRef as T
    }
}