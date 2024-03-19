package com.huawei.cangjie.descriptors.rendering

interface DiagnosticParameterRenderer<in O> {
    fun render(obj: O, renderingContext: RenderingContext): String
}
