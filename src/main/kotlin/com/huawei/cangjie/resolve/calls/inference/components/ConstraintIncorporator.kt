package com.huawei.cangjie.resolve.calls.inference.components


import com.huawei.cangjie.resolve.calls.inference.model.Constraint
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

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    fun incorporate(c: Context, typeVariable: TypeVariableMarker, constraint: Constraint) {
//        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
//
//        // we shouldn't incorporate recursive constraint -- It is too dangerous
//        if (c.areThereRecursiveConstraints(typeVariable, constraint)) return
//
//        c.directWithVariable(typeVariable, constraint)
//        c.insideOtherConstraint(typeVariable, constraint)
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
