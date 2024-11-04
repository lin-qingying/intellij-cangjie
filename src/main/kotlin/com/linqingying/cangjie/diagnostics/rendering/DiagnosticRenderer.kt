package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.cangjie.diagnostics.UnboundDiagnostic


interface DiagnosticRenderer<in D : UnboundDiagnostic> {
    fun render(diagnostic: D): String

    fun renderParameters(diagnostic: D): Array<out Any?>
}
