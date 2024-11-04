package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.container.PlatformSpecificExtension
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.renderer.DescriptorRenderer

data class DeclarationWithDiagnosticComponents(
    val declaration: DeclarationDescriptor,
    val diagnosticComponents: PlatformSpecificDiagnosticComponents
) : Iterable<Any> {
    override fun iterator() =
        sequenceOf(declaration, diagnosticComponents).iterator()
}

fun DescriptorRenderer.withAnnotationsWhitelist(
    toParameterRenderer: DescriptorRenderer.() -> DiagnosticParameterRenderer<DeclarationDescriptor> = DescriptorRenderer::asRenderer
) = AnnotationsWhitelistDescriptorRenderer(this, toParameterRenderer)

class AnnotationsWhitelistDescriptorRenderer(
    private val baseRenderer: DescriptorRenderer,
    private val toParameterRenderer: DescriptorRenderer.() -> DiagnosticParameterRenderer<DeclarationDescriptor>
) : DiagnosticParameterRenderer<DeclarationWithDiagnosticComponents> {
    override fun render(obj: DeclarationWithDiagnosticComponents, renderingContext: RenderingContext): String {
        val (descriptor, diagnosticComponents) = obj
        return baseRenderer.withOptions {
            annotationFilter = { annotation ->
                diagnosticComponents.isNullabilityAnnotation(annotation, descriptor)
            }
        }.toParameterRenderer().render(descriptor, renderingContext)
    }
}

@DefaultImplementation(impl = PlatformSpecificDiagnosticComponents.Default::class)
interface PlatformSpecificDiagnosticComponents : PlatformSpecificExtension<PlatformSpecificDiagnosticComponents> {
    fun isNullabilityAnnotation(
        annotationDescriptor: AnnotationDescriptor,
        containingDeclaration: DeclarationDescriptor
    ): Boolean

    object Default : PlatformSpecificDiagnosticComponents {
        override fun isNullabilityAnnotation(
            annotationDescriptor: AnnotationDescriptor,
            containingDeclaration: DeclarationDescriptor
        ): Boolean = false
    }
}
