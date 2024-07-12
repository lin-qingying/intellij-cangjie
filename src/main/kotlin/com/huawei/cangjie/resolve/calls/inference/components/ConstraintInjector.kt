package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.ForkPointData
import com.huawei.cangjie.resolve.calls.inference.model.*
import com.huawei.cangjie.types.AbstractTypeApproximator
import com.huawei.cangjie.types.model.*
fun CangJieTypeMarker.typeConstructor(context: TypeSystemContext): TypeConstructorMarker =
    with(context) { typeConstructor() }

class ConstraintInjector(
    val constraintIncorporator: ConstraintIncorporator,
    val typeApproximator: AbstractTypeApproximator,
//    private val languageVersionSettings: LanguageVersionSettings,
) {
    fun processGivenForkPointBranchConstraints(
        c: Context,
        constraintSet: Collection<Pair<TypeVariableMarker, Constraint>>,
        position: IncorporationConstraintPosition
    ) {
//        processGivenConstraints(
//            c,
//            TypeCheckerStateForConstraintInjector(c, position),
//            constraintSet,
//        )
    }
    fun processMissedConstraints(
        c: Context,
        position: IncorporationConstraintPosition,
        missedConstraints: List<Pair<TypeVariableMarker, Constraint>>
    ) {
//        val properConstraintsProcessingEnabled =
//            languageVersionSettings.supportsFeature(LanguageFeature.ProperTypeInferenceConstraintsProcessing)
//
//        // If proper constraints processing is enabled, then we don't have missed constraints
//        if (properConstraintsProcessingEnabled) return
//
//        val typeCheckerState = TypeCheckerStateForConstraintInjector(c, position)
//        for ((variable, constraint) in missedConstraints) {
//            typeCheckerState.addPossibleNewConstraint(variable, constraint)
//        }
//        processConstraints(c, typeCheckerState, skipProperEqualityConstraints = false)
    }
    fun addInitialEqualityConstraint(c: Context, a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition) = with(c) {
//        val (typeVariable, equalType) = when {
//            a.typeConstructor(c) is TypeVariableTypeConstructorMarker -> a to b
//            b.typeConstructor(c) is TypeVariableTypeConstructorMarker -> b to a
//            else -> return
//        }
//        val initialConstraint = InitialConstraint(typeVariable, equalType, EQUALITY, position).also { c.addInitialConstraint(it) }
//        val typeCheckerState = TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))
//
//        // We add constraints like `T? == Foo!` in the old way
//        if (!typeVariable.isSimpleType() || typeVariable.isMarkedNullable()) {
//            addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType, typeCheckerState)
//            return
//        }
//
//        updateAllowedTypeDepth(c, equalType)
//        addEqualityConstraintAndIncorporateIt(c, typeVariable, equalType, typeCheckerState)
    }

    fun addInitialSubtypeConstraint(
        c: Context,
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        position: ConstraintPosition
    ) {
//        val initialConstraint = InitialConstraint(lowerType, upperType, UPPER, position).also { c.addInitialConstraint(it) }
//        val typeCheckerState = TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))
//
//        updateAllowedTypeDepth(c, lowerType)
//        updateAllowedTypeDepth(c, upperType)
//
//        addSubTypeConstraintAndIncorporateIt(c, lowerType, upperType, typeCheckerState)
    }

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        var maxTypeDepthFromInitialConstraints: Int
        val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>
        val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker>
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        val atCompletionState: Boolean

        fun addInitialConstraint(initialConstraint: InitialConstraint)
        fun addError(error: ConstraintSystemError)

        fun addMissedConstraints(
            position: IncorporationConstraintPosition,
            constraints: MutableList<Pair<TypeVariableMarker, Constraint>>
        )

        fun resolveForkPointsConstraints()
    }
}
