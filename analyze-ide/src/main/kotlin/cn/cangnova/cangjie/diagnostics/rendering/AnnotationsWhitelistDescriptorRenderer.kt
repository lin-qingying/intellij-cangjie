/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.diagnostics.rendering

import cn.cangnova.cangjie.container.DefaultImplementation
import cn.cangnova.cangjie.container.PlatformSpecificExtension
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import cn.cangnova.cangjie.renderer.DescriptorRenderer

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
