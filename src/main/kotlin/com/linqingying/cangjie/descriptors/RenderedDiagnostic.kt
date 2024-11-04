package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.DiagnosticFactory
import com.linqingying.cangjie.diagnostics.rendering.DiagnosticRenderer

class RenderedDiagnostic<D : Diagnostic>(
    val diagnostic: D,
    val renderer: DiagnosticRenderer<D>
) {
    val text = renderer.render(diagnostic)

    val factory: DiagnosticFactory<*> get() = diagnostic.factory

    override fun toString() = text
}
