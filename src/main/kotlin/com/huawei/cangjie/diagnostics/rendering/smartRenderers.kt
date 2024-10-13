package com.huawei.cangjie.diagnostics.rendering

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.types.CangJieType

class SmartTypeRenderer(private val baseRenderer: DescriptorRenderer) : DiagnosticParameterRenderer<CangJieType> {
    override fun render(obj: CangJieType, renderingContext: RenderingContext): String {
        val adaptiveRenderer = baseRenderer.withOptions {
            classifierNamePolicy = renderingContext.adaptiveClassifierPolicy
        }
        return adaptiveRenderer.renderType(obj)
    }
}

class SmartDescriptorRenderer(private val baseRenderer: DescriptorRenderer) : DiagnosticParameterRenderer<DeclarationDescriptor> {
    override fun render(obj: DeclarationDescriptor, renderingContext: RenderingContext): String {
        val adaptiveRenderer = baseRenderer.withOptions {
            classifierNamePolicy = renderingContext.adaptiveClassifierPolicy
        }
        return adaptiveRenderer.render(obj)
    }
}

