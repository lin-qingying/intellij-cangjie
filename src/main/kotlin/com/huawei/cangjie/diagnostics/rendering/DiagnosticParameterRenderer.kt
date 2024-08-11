package com.huawei.cangjie.diagnostics.rendering

interface DiagnosticParameterRenderer<in O> {
    fun render(obj: O, renderingContext: RenderingContext): String
}
fun <O> ContextDependentRenderer(block: (O, RenderingContext) -> String) = object : DiagnosticParameterRenderer<O> {
    override fun render(obj: O, renderingContext: RenderingContext): String = block(obj, renderingContext)
}
