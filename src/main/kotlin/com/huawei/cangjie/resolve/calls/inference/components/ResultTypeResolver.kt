package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.huawei.cangjie.types.AbstractTypeApproximator
import com.huawei.cangjie.types.model.*

class ResultTypeResolver(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
//    private val languageVersionSettings: LanguageVersionSettings
){

    interface Context : TypeSystemInferenceExtensionContext {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val outerSystemVariablesPrefixSize: Int
        fun isProperType(type: CangJieTypeMarker): Boolean
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
        fun isReified(variable: TypeVariableMarker): Boolean
    }
}
