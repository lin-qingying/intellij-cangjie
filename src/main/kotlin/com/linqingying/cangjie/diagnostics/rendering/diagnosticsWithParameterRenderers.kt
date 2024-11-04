package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.cangjie.diagnostics.DiagnosticWithParameters1
import com.linqingying.cangjie.diagnostics.DiagnosticWithParameters4

class DiagnosticWithParametersMultiRenderer<A:Any>(
    message: String,
    private val renderer: MultiRenderer<A>
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters1<*, A>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters1<*, A>): Array<out Any> {
        return renderer.render(diagnostic.a)
    }
}
class DiagnosticWithParameters4Renderer<A :Any , B :Any , C :Any, D :Any>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A> ?,
    private val rendererForB: DiagnosticParameterRenderer<B> ?,
    private val rendererForC: DiagnosticParameterRenderer<C> ?,
    private val rendererForD: DiagnosticParameterRenderer<D> ?,
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters4<*, A, B, C, D>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters4<*, A, B, C, D>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c, diagnostic.d)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
            renderParameter(diagnostic.c, rendererForC, context),
            renderParameter(diagnostic.d, rendererForD, context),
        )
    }
}

interface MultiRenderer<in A> {
    fun render(a: A): Array<String>
}
