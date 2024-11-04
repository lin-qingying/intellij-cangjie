package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.linqingying.cangjie.types.model.*

interface PostponedArgumentsAnalyzerContext : TypeSystemInferenceExtensionContext{

    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker
    fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
    fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker>

    // type can be proper if it not contains not fixed type variables
    fun canBeProper(type: CangJieTypeMarker): Boolean

    fun hasUpperOrEqualUnitConstraint(type: CangJieTypeMarker): Boolean

    fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>)

    // mutable operations
    fun addOtherSystem(otherSystem: ConstraintStorage)

    fun getBuilder(): ConstraintSystemBuilder
    fun resolveForkPointsConstraints()
}
