package com.huawei.cangjie.resolve.calls.inference.components


import com.huawei.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.huawei.cangjie.resolve.calls.inference.model.Constraint
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintKind
import com.huawei.cangjie.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.huawei.cangjie.types.AbstractTypeApproximator
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeConstructorMarker
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContext
import com.huawei.cangjie.types.model.TypeVariableMarker

// todo problem: intersection types in constrains: A <: Number, B <: Inv<A & Any> =>? B <: Inv<out Number & Any>
class ConstraintIncorporator(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    val utilContext: ConstraintSystemUtilContext
)
{
    // A <:(=) \alpha <:(=) B => A <: B
    private fun Context.directWithVariable(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
        val shouldBeTypeVariableFlexible =
            if (useRefinedBoundsForTypeVariableInFlexiblePosition())
                false
            else
                with(utilContext) { typeVariable.shouldBeFlexible() }

        // \alpha <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            forEachConstraint(typeVariable) {
                if (it.kind != ConstraintKind.UPPER) {
                    addNewIncorporatedConstraint(it.type, constraint.type, shouldBeTypeVariableFlexible, it.isNullabilityConstraint)
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            forEachConstraint(typeVariable) {
                if (it.kind != ConstraintKind.LOWER) {
                    val isFromDeclaredUpperBound =
                        it.position.from is DeclaredUpperBoundConstraintPosition<*> && !it.type.typeConstructor().isTypeVariable()

                    addNewIncorporatedConstraint(
                        constraint.type,
                        it.type,
                        shouldBeTypeVariableFlexible,
                        isFromDeclaredUpperBound = isFromDeclaredUpperBound
                    )
                }
            }
        }
    }
    private fun Context.areThereRecursiveConstraints(typeVariable: TypeVariableMarker, constraint: Constraint) =
        constraint.type.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable.freshTypeConstructor() }

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    fun incorporate(c: Context, typeVariable: TypeVariableMarker, constraint: Constraint) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        // we shouldn't incorporate recursive constraint -- It is too dangerous
        if (c.areThereRecursiveConstraints(typeVariable, constraint)) return

        c.directWithVariable(typeVariable, constraint)
//        c.insideOtherConstraint(typeVariable, constraint)
    }

    private fun Context.insideOtherConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
//        val freshTypeConstructor = typeVariable.freshTypeConstructor()
//        for (typeVariableWithConstraint in this@insideOtherConstraint.allTypeVariablesWithConstraints) {
//            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
//                containsTypeVariable(it.type, freshTypeConstructor)
//            }
//            constraintsWhichConstraintMyVariable.forEach {
//                generateNewConstraint(typeVariableWithConstraint.typeVariable, it, typeVariable, constraint)
//            }
//        }
    }

    private inline fun Context.forEachConstraint(typeVariable: TypeVariableMarker, action: (Constraint) -> Unit) {
        // We use an indexed loop because the collection might be modified during the iteration.
        // However, the only modification is appending, so we should be fine.
        val constraints = getConstraintsForVariable(typeVariable)
        var i = 0
        while (i < constraints.size) {
            action(constraints[i++])
        }
    }

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        // if such type variable is fixed then it is error
        fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker?

        fun getConstraintsForVariable(typeVariable: TypeVariableMarker): List<Constraint>

        fun addNewIncorporatedConstraint(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            isFromNullabilityConstraint: Boolean = false,
            isFromDeclaredUpperBound: Boolean = false
        )

        fun addNewIncorporatedConstraint(typeVariable: TypeVariableMarker, type: CangJieTypeMarker, constraintContext: ConstraintContext)
    }
}
