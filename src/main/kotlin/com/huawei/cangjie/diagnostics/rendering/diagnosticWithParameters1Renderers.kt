package com.huawei.cangjie.diagnostics.rendering

import com.huawei.cangjie.diagnostics.UnboundDiagnostic
import com.huawei.cangjie.diagnostics.DiagnosticWithParameters1
import com.huawei.cangjie.diagnostics.DiagnosticWithParameters2
import com.huawei.cangjie.diagnostics.DiagnosticWithParameters3
import java.text.MessageFormat


interface ContextIndependentParameterRenderer<in O> : DiagnosticParameterRenderer<O> {
    override fun render(obj: O, renderingContext: RenderingContext): String = render(obj)

    fun render(obj: O): String
}

fun <P> renderParameter(parameter: P, renderer: DiagnosticParameterRenderer<P>?, context: RenderingContext): Any? =
    renderer?.render(parameter, context) ?: parameter

fun <O> renderer(block: (O) -> String) = object : ContextIndependentParameterRenderer<O> {
    override fun render(obj: O): String = block(obj)
}
abstract class AbstractDiagnosticWithParametersRenderer<in D : UnboundDiagnostic> protected constructor(message: String) :
    DiagnosticRenderer<D> {
    private val messageFormat = MessageFormat(message)

    override fun render(diagnostic: D): String {
        return messageFormat.format(renderParameters(diagnostic))
    }

    override fun renderParameters(diagnostic: D): Array<out Any?> {
        return arrayOf()
    }
}

class DiagnosticWithParameters1Renderer<A>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters1<*, A>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters1<*, A>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a)
        return arrayOf(renderParameter(diagnostic.a, rendererForA, context))
    }
}
class DiagnosticWithParameters2Renderer<A, B>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters2<*, A, B>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters2<*, A, B>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context)
        )
    }
}

class DiagnosticWithParameters3Renderer<A, B, C>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters3<*, A, B, C>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters3<*, A, B, C>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
            renderParameter(diagnostic.c, rendererForC, context)
        )
    }
}
