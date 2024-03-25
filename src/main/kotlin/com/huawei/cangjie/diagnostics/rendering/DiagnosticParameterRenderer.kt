package com.huawei.cangjie.diagnostics.rendering

interface DiagnosticParameterRenderer<in O> {
    fun render(obj: O, renderingContext: RenderingContext): String
}
