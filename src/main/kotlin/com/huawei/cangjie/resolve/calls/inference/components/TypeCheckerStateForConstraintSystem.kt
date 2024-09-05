package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.types.AbstractTypeRefiner
import com.huawei.cangjie.types.TypeCheckerState
import com.huawei.cangjie.types.checker.AbstractTypePreparator
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContext

abstract class TypeCheckerStateForConstraintSystem(
    val extensionTypeContext: TypeSystemInferenceExtensionContext,
    cangjieTypePreparator: AbstractTypePreparator,
    cangjieTypeRefiner: AbstractTypeRefiner
) : TypeCheckerState(
    isErrorTypeEqualsToAnything = true,
    isStubTypeEqualsToAnything = true,
    allowedTypeVariable = false,
    typeSystemContext = extensionTypeContext,
    cangjieTypePreparator,
    cangjieTypeRefiner
)
