package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.psi.CjAnnotationEntry
import com.huawei.cangjie.psi.CjModifierList
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.types.CangJieType


abstract class AnnotationResolver {
    fun resolveAnnotationsWithoutArguments(
        scope: LexicalScope,
        modifierList: CjModifierList?,
        trace: BindingTrace
    ): Annotations = resolveAnnotationsFromModifierList(scope, modifierList, trace, false)

    fun resolveAnnotationsWithArguments(
        scope: LexicalScope,
        modifierList: CjModifierList?,
        trace: BindingTrace
    ): Annotations = resolveAnnotationsFromModifierList(scope, modifierList, trace, true)


    private fun resolveAnnotationsFromModifierList(
        scope: LexicalScope,
        modifierList: CjModifierList?,
        trace: BindingTrace,
        shouldResolveArguments: Boolean
    ): Annotations {
        if (modifierList == null) {
            return Annotations.EMPTY
        }

        return resolveAnnotationEntries(scope, modifierList.annotationEntries, trace, shouldResolveArguments)
    }

    fun resolveAnnotationsWithoutArguments(
        scope: LexicalScope,
        annotationEntries: @JvmSuppressWildcards List<CjAnnotationEntry>,
        trace: BindingTrace
    ): Annotations = resolveAnnotationEntries(scope, annotationEntries, trace, false)

    fun resolveAnnotationsWithArguments(
        scope: LexicalScope,
        annotationEntries: @JvmSuppressWildcards List<CjAnnotationEntry>,
        trace: BindingTrace
    ): Annotations = resolveAnnotationEntries(scope, annotationEntries, trace, true)

    protected abstract fun resolveAnnotationEntries(
        scope: LexicalScope,
        annotationEntries: @JvmSuppressWildcards List<CjAnnotationEntry>,
        trace: BindingTrace,
        shouldResolveArguments: Boolean
    ): Annotations


    abstract fun resolveAnnotationType(scope: LexicalScope, entryElement: CjAnnotationEntry, trace: BindingTrace): CangJieType
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
