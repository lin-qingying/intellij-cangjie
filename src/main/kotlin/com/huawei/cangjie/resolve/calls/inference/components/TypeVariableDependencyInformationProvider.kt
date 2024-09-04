package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.huawei.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeConstructorMarker

class TypeVariableDependencyInformationProvider(
    private val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>,
    private val postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
    private val topLevelType: CangJieTypeMarker?,
    private val typeSystemContext: VariableFixationFinder.Context
)
{
    private val deepTypeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    fun getDeeplyDependentVariables(variable: TypeConstructorMarker) = deepTypeVariableDependencies[variable]

}
