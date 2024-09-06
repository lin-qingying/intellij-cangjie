package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.resolve.calls.inference.model.ConstraintKind
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
// if runOperations return true, then this operation will be applied, and function return true
inline fun ConstraintSystemBuilder.runTransaction(crossinline runOperations: ConstraintSystemOperation.() -> Boolean): Boolean {
    val transactionState = prepareTransaction()

    // typeVariablesTransaction is clear
    if (runOperations()) {
        transactionState.closeTransaction()
        return true
    }

    transactionState.rollbackTransaction()
    return false
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
fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.LOWER)
private fun ConstraintSystemBuilder.addConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition,
    kind: ConstraintKind
): Boolean = runTransaction {
    if (!hasContradiction) {
        when (kind) {
            ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
            ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
            ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
        }
    }
    !hasContradiction
}
fun ConstraintSystemBuilder.addEqualityConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.EQUALITY)
