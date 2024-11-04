package com.linqingying.cangjie.progress

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
open class CompilationCanceledException : ProcessCanceledException()

class IncrementalNextRoundException : CompilationCanceledException()

interface CompilationCanceledStatus {
    fun checkCanceled(): Unit
}
object ProgressIndicatorAndCompilationCanceledStatus {
    private var canceledStatus: CompilationCanceledStatus? = null

    @JvmStatic
    @Synchronized fun setCompilationCanceledStatus(newCanceledStatus: CompilationCanceledStatus?): Unit {
        canceledStatus = newCanceledStatus
    }

    @JvmStatic fun checkCanceled(): Unit {
        ProgressIndicatorProvider.checkCanceled()
        canceledStatus?.checkCanceled()
    }
}
