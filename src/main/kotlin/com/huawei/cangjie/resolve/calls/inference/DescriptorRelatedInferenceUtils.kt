package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContext

fun ConstraintStorage.buildResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): NewTypeSubstitutor {
    return buildAbstractResultingSubstitutor(context, transformTypeVariablesToErrorTypes) as NewTypeSubstitutor
}
