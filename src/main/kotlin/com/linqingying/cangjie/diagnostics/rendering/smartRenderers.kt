package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.types.CangJieType

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

