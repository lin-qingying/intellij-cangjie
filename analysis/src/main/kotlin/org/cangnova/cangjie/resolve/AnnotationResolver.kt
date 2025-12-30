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

import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjAnnotations
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType


abstract class AnnotationResolver {
    fun resolveAnnotationsWithoutArguments(
        scope: LexicalScope,
        modifierList: CjAnnotations?,
        trace: BindingTrace
    ): Annotations = resolveAnnotationsFromModifierList(scope, modifierList, trace, false)

    fun resolveAnnotationsWithArguments(
        scope: LexicalScope,
        modifierList: CjAnnotations?,
        trace: BindingTrace
    ): Annotations = resolveAnnotationsFromModifierList(scope, modifierList, trace, true)


    private fun resolveAnnotationsFromModifierList(
        scope: LexicalScope,
        modifierList: CjAnnotations?,
        trace: BindingTrace,
        shouldResolveArguments: Boolean
    ): Annotations {
        if (modifierList == null) {
            return Annotations.EMPTY
        }

        return resolveAnnotationEntries(scope, modifierList.entries, trace, shouldResolveArguments)
    }

    fun resolveAnnotationsWithoutArguments(
        scope: LexicalScope,
        annotationEntries: @JvmSuppressWildcards List<CjAnnotation>,
        trace: BindingTrace
    ): Annotations = resolveAnnotationEntries(scope, annotationEntries, trace, false)

    fun resolveAnnotationsWithArguments(
        scope: LexicalScope,
        annotationEntries: @JvmSuppressWildcards List<CjAnnotation>,
        trace: BindingTrace
    ): Annotations = resolveAnnotationEntries(scope, annotationEntries, trace, true)

    protected abstract fun resolveAnnotationEntries(
        scope: LexicalScope,
        annotationEntries: @JvmSuppressWildcards List<CjAnnotation>,
        trace: BindingTrace,
        shouldResolveArguments: Boolean
    ): Annotations


    abstract fun resolveAnnotationType(
        scope: LexicalScope,
        entryElement: CjAnnotation,
        trace: BindingTrace
    ): CangJieType
//
//    abstract fun resolveAnnotationCall(
//        annotationEntry: CjAnnotationEntry,
//        scope: LexicalScope,
//        trace: BindingTrace
//    ): OverloadResolutionResults<FunctionDescriptor>
//
//    abstract fun getAnnotationArgumentValue(
//        trace: BindingTrace,
//        valueParameter: ValueParameterDescriptor,
//        resolvedArgument: ResolvedValueArgument
//    ): ConstantValue<*>?
}
