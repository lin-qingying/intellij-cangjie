package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.Diagnostic


class TrackingBindingTrace(val trace: BindingTrace) : BindingTrace by trace {
    var reported: Boolean = false

    override fun report(diagnostic: Diagnostic) {
        if (bindingContext.diagnostics.noSuppression().forElement(diagnostic.psiElement).any { it == diagnostic }) return

        trace.report(diagnostic)
        reported = true
    }

    fun markAsReported() {
        reported = true
    }
}
