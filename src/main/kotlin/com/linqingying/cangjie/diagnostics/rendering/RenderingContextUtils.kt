package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.*
import com.linqingying.cangjie.renderer.DescriptorRenderer

fun RenderingContext.Companion.fromDiagnostic(d: Diagnostic): RenderingContext = RenderingContext.Impl(parameters(d))

fun RenderingContext.Companion.parameters(d: Diagnostic): List<Any> = when (d) {
    is SimpleDiagnostic<*> -> listOf()
    is DiagnosticWithParameters1<*, *> -> listOf(d.a)
    is DiagnosticWithParameters2<*, *, *> -> listOf(d.a, d.b)
    is DiagnosticWithParameters3<*, *, *, *> -> listOf(d.a, d.b, d.c)
    is DiagnosticWithParameters4<*, *, *, *, *> -> listOf(d.a, d.b, d.c, d.d)
    is ParametrizedDiagnostic<*> -> error("Unexpected diagnostic: ${d::class.java}")
    else -> listOf()
}
fun ClassifierDescriptorWithTypeParameters.renderKindWithName(): String =
    DescriptorRenderer.getClassifierKindPrefix(this) + " '" + name + "'"
