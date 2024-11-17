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

package com.linqingying.cangjie.descriptors

import com.google.common.collect.ImmutableSet
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.Severity
import com.linqingying.cangjie.psi.CjAnnotated
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.psiUtil.CjStubbedPsiUtil
import com.linqingying.cangjie.psi.doNotAnalyze
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.constants.ArrayValue
import com.linqingying.cangjie.resolve.constants.StringValue
import com.linqingying.cangjie.utils.ExtensionProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement

abstract class CangJieSuppressCache : AbstractCangJieSuppressCache<PsiElement>() {
    private val diagnosticSuppressors = ExtensionProvider.create(DiagnosticSuppressor.EP_NAME)
    protected open fun isSuppressedByExtension(suppressor: DiagnosticSuppressor, diagnostic: Diagnostic): Boolean {
        return suppressor.isSuppressed(diagnostic)
    }

    val filter: (Diagnostic) -> Boolean = { diagnostic: Diagnostic ->
        !isSuppressed(DiagnosticSuppressRequest(diagnostic))
    }

    override fun isSuppressed(request: SuppressRequest<PsiElement>): Boolean {
              val element = request.element
        if (!element.isValid) return true

        val file = element.containingFile
        if (file is CjFile) {
            if (file.doNotAnalyze != null) return true
        }

        if (request is DiagnosticSuppressRequest) {
            for (suppressor in diagnosticSuppressors.get()) {
                if (isSuppressedByExtension(suppressor, request.diagnostic)) return true
            }
        }
        return super.isSuppressed(request)
    }

    abstract fun getSuppressionAnnotations(annotated: PsiElement): List<AnnotationDescriptor>

    private fun processAnnotation(builder: ImmutableSet.Builder<String>, annotationDescriptor: AnnotationDescriptor) {
//        if (annotationDescriptor.fqName != StandardNames.FqNames.suppress) return

        // We only add strings and skip other values to facilitate recovery in presence of erroneous code
        for (arrayValue in annotationDescriptor.allValueArguments.values) {
            if (arrayValue is ArrayValue) {
                for (value in arrayValue.value) {
                    if (value is StringValue) {
                        builder.add(value.value.lowercase())
                    }
                }
            }
        }
    }

    override fun getClosestAnnotatedAncestorElement(
        element: PsiElement,
        rootElement: PsiElement,
        excludeSelf: Boolean
    ): PsiElement? =
        CjStubbedPsiUtil.getPsiOrStubParent(element, CjAnnotated::class.java, excludeSelf)

    override fun getSuppressingStrings(annotated: PsiElement): Set<String> {
        val builder = ImmutableSet.builder<String>()

        for (annotationDescriptor in getSuppressionAnnotations(annotated)) {
            processAnnotation(builder, annotationDescriptor)
        }

        return builder.build()
    }

    companion object {
        internal fun getDiagnosticSuppressKey(diagnostic: Diagnostic): String =
            diagnostic.factory.name.lowercase()
    }

    protected class DiagnosticSuppressRequest(val diagnostic: Diagnostic) : SuppressRequest<PsiElement> {
        override val element: PsiElement get() = diagnostic.psiElement
        override val rootElement: PsiElement get() = element.containingFile
        override val severity: Severity get() = diagnostic.severity
        override val suppressKey: String get() = getDiagnosticSuppressKey(diagnostic)
    }
}

interface DiagnosticSuppressor {
    fun isSuppressed(diagnostic: Diagnostic): Boolean
    fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean = isSuppressed(diagnostic)

    companion object {
        val EP_NAME: ExtensionPointName<DiagnosticSuppressor> =
            ExtensionPointName.create("com.linqingying.cangjie.diagnosticSuppressor")
    }
}

class BindingContextSuppressCache(val context: BindingContext) : CangJieSuppressCache() {
//    override fun getSuppressionAnnotations(annotated: PsiElement): List<AnnotationDescriptor> {
//        val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, annotated)
//
//        return descriptor?.annotations?.toList()
//            ?: (annotated as? CjAnnotated)?.annotationEntries?.mapNotNull { context.get(BindingContext.ANNOTATION, it) }
//            ?: emptyList()
//    }

    override fun isSuppressedByExtension(suppressor: DiagnosticSuppressor, diagnostic: Diagnostic): Boolean {
        return suppressor.isSuppressed(diagnostic, context)
    }

    override fun getSuppressionAnnotations(annotated: PsiElement): List<AnnotationDescriptor> {
        val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, annotated)
        return descriptor?.annotations?.toList()
            ?: (annotated as? CjAnnotated)?.annotationEntries?.mapNotNull { context.get(BindingContext.ANNOTATION, it) }
            ?: emptyList()
    }


}

