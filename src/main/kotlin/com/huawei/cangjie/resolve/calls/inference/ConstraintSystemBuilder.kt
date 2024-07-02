package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.resolve.calls.inference.model.ConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeConstructorMarker
import com.huawei.cangjie.types.model.TypeSubstitutorMarker
import com.huawei.cangjie.types.model.TypeVariableMarker


interface ConstraintSystemOperation {
    val hasContradiction: Boolean
    fun registerVariable(variable: TypeVariableMarker)
    fun markPostponedVariable(variable: TypeVariableMarker)
    fun markCouldBeResolvedWithUnrestrictedBuilderInference()
    fun unmarkPostponedVariable(variable: TypeVariableMarker)
    fun removePostponedVariables()
    fun substituteFixedVariables(substitutor: TypeSubstitutorMarker)

    fun getBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>
    ): CangJieTypeMarker?

    fun getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable: TypeConstructorMarker): CangJieTypeMarker?

    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: CangJieTypeMarker
    )

    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: CangJieTypeMarker
    )

    fun addSubtypeConstraint(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker, position: ConstraintPosition)
    fun addEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition)

    fun isProperType(type: CangJieTypeMarker): Boolean
    fun isTypeVariable(type: CangJieTypeMarker): Boolean
    fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean

    fun getProperSuperTypeConstructors(type: CangJieTypeMarker): List<TypeConstructorMarker>

    fun addOtherSystem(otherSystem: ConstraintStorage)

    val errors: List<ConstraintSystemError>
}

abstract class ConstraintSystemTransaction {
    abstract fun closeTransaction()

    abstract fun rollbackTransaction()
}
interface ConstraintSystemBuilder : ConstraintSystemOperation {
    fun prepareTransaction(): ConstraintSystemTransaction

    fun buildCurrentSubstitutor(): TypeSubstitutorMarker

    fun currentStorage(): ConstraintStorage
}
