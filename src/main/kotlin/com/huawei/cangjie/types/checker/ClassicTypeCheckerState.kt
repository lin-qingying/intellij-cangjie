package com.huawei.cangjie.types.checker

import com.huawei.cangjie.types.TypeCheckerState


fun createClassicTypeCheckerState(
    isErrorTypeEqualsToAnything: Boolean,
    isStubTypeEqualsToAnything: Boolean = true,
    typeSystemContext: ClassicTypeSystemContext = SimpleClassicTypeSystemContext,
    cangjieTypePreparator:CangJieTypePreparator =CangJieTypePreparator.Default,
    cangjieTypeRefiner:CangJieTypeRefiner =CangJieTypeRefiner.Default
): TypeCheckerState {
    return TypeCheckerState(
        isErrorTypeEqualsToAnything,
        isStubTypeEqualsToAnything,
        allowedTypeVariable = true,
        typeSystemContext,
        cangjieTypePreparator,
        cangjieTypeRefiner
    )
}
