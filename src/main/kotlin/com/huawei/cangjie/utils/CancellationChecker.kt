package com.huawei.cangjie.utils

import com.intellij.openapi.progress.ProgressManager

interface CancellationChecker {
    fun check()
}
object ProgressManagerBasedCancellationChecker : CancellationChecker {
    override fun check() {
        ProgressManager.checkCanceled()
    }
}
