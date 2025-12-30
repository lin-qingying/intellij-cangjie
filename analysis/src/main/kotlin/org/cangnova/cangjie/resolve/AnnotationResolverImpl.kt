/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.TargetedAnnotations
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.error.ErrorTypeKind

class AnnotationResolverImpl(
    private val callResolver: CallResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val storageManager: StorageManager
) : AnnotationResolver() {

    private lateinit var typeResolver: TypeResolver

    override fun resolveAnnotationEntries(
        scope: LexicalScope,
        annotationEntryElements: List<CjAnnotation>,
        trace: BindingTrace,
        shouldResolveArguments: Boolean
    ): Annotations {
        if (annotationEntryElements.isEmpty()) return Annotations.EMPTY

        val standard = mutableListOf<AnnotationDescriptor>()
        val targeted = mutableListOf<org.cangnova.cangjie.descriptors.annotations.AnnotationWithTarget>()

//        for (entryElement in annotationEntryElements) {
//            var descriptor = trace.get(BindingContext.ANNOTATION, entryElement)
//            if (descriptor == null) {
//                val lazyAnnotationDescriptor =
//                    LazyAnnotationDescriptor(LazyAnnotationsContextImpl(this, storageManager, trace, scope), entryElement)
//                descriptor = lazyAnnotationDescriptor
//            }
//            if (shouldResolveArguments) {
//                ForceResolveUtil.forceResolveAllContents(descriptor)
//            }
//
//            val target = entryElement.useSiteTarget
//            if (target != null) {
//                targeted.add(AnnotationWithTarget(descriptor, target.annotationUseSiteTarget))
//            } else {
//                standard.add(descriptor)
//            }
//        }
        return TargetedAnnotations(standard.toList(), targeted.toList())
    }

    override fun resolveAnnotationType(
        scope: LexicalScope,
        entryElement: CjAnnotation,
        trace: BindingTrace
    ): CangJieType {
        val typeReference = entryElement.typeReference
            ?: return ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, entryElement.text)

        val type = typeResolver.resolveType(scope, typeReference, trace, true)
        if (type.constructor.declarationDescriptor !is ClassDescriptor) {
            return ErrorUtils.createErrorType(ErrorTypeKind.NOT_ANNOTATION_TYPE_IN_ANNOTATION_CONTEXT, type.toString())
        }
        return type
    }
}
