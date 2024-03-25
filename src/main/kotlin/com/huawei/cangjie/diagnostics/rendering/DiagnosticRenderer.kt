package com.huawei.cangjie.diagnostics.rendering

import com.huawei.cangjie.diagnostics.UnboundDiagnostic


interface DiagnosticRenderer<in D : UnboundDiagnostic> {
    fun render(diagnostic: D): String

    fun renderParameters(diagnostic: D): Array<out Any?>
}
