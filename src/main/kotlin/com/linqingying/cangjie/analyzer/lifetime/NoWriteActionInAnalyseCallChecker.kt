package com.linqingying.cangjie.analyzer.lifetime

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager


class NoWriteActionInAnalyseCallChecker(parentDisposable: Disposable) {
    init {
        val listener = object : ApplicationListener {
            override fun writeActionFinished(action: Any) {
                if (currentAnalysisContextEnteringCount.get() > 0) {
                    throw WriteActionStartInsideAnalysisContextException()
                }
            }
        }
        ApplicationManager.getApplication().addApplicationListener(listener, parentDisposable)
    }

    fun beforeEnteringAnalysisContext() {
        currentAnalysisContextEnteringCount.set(currentAnalysisContextEnteringCount.get() + 1)
    }

    fun afterLeavingAnalysisContext() {
        currentAnalysisContextEnteringCount.set(currentAnalysisContextEnteringCount.get() - 1)
    }

    private val currentAnalysisContextEnteringCount = ThreadLocal.withInitial { 0 }
}

class WriteActionStartInsideAnalysisContextException : IllegalStateException(
    "write action should be never executed inside analysis context (e,g. analyse call)"
)
