package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.diagnostics.Diagnostic


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
