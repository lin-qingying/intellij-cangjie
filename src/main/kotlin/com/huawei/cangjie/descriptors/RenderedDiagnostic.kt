package com.huawei.cangjie.descriptors

import com.huawei.cangjie.diagnostics.rendering.DiagnosticRenderer

class RenderedDiagnostic<D : Diagnostic>(
    val diagnostic: D,
    val renderer: DiagnosticRenderer<D>
) {
    val text = renderer.render(diagnostic)

    val factory: DiagnosticFactory<*> get() = diagnostic.factory

    override fun toString() = text
}
