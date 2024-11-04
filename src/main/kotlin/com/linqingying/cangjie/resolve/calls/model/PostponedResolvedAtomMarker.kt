package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.types.model.CangJieTypeMarker

interface PostponedResolvedAtomMarker {
    val inputTypes: Collection<CangJieTypeMarker>
    val outputType: CangJieTypeMarker?
    val expectedType: CangJieTypeMarker?
    val analyzed: Boolean
}
interface PostponedAtomWithRevisableExpectedType : PostponedResolvedAtomMarker {
    val revisedExpectedType: CangJieTypeMarker?

    fun reviseExpectedType(expectedType: CangJieTypeMarker)
}
interface PostponedCallableReferenceMarker : PostponedAtomWithRevisableExpectedType
interface LambdaWithTypeVariableAsExpectedTypeMarker : PostponedAtomWithRevisableExpectedType {
    val parameterTypesFromDeclaration: List<CangJieTypeMarker?>?

    fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?)
}
