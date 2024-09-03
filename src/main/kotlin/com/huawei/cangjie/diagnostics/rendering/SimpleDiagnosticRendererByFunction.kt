package com.huawei.cangjie.diagnostics.rendering

import com.huawei.cangjie.descriptors.Diagnostic


class SimpleDiagnosticRendererByFunction(private val message: () -> String) :
    DiagnosticRenderer<Diagnostic> {
    override fun render(diagnostic: Diagnostic): String {
        return message()
    }

    override fun renderParameters(diagnostic: Diagnostic): Array<Any?> {
        return arrayOfNulls(0)
    }
}
