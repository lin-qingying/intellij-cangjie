//package com.huawei.cangjie.types.checker
//
//import com.huawei.cangjie.types.CangJieTypeMarker
//import com.huawei.cangjie.types.TypeCheckerState
//
//object AbstractTypeChecker {
//    fun equalTypes(state: TypeCheckerState, a: CangJieTypeMarker, b: CangJieTypeMarker): Boolean =
//        with(state.typeSystemContext) {
//            if (a === b) return true
//
////            if (isCommonDenotableType(a) && isCommonDenotableType(b)) {
////                val refinedA = state.prepareType(state.refineType(a))
////                val refinedB = state.prepareType(state.refineType(b))
////                val simpleA = refinedA.lowerBoundIfFlexible()
////                if (!areEqualTypeConstructors(refinedA.typeConstructor(), refinedB.typeConstructor())) return false
////                if (simpleA.argumentsCount() == 0) {
////                    if (refinedA.hasFlexibleNullability() || refinedB.hasFlexibleNullability()) return true
////
////                    return simpleA.isMarkedNullable() == refinedB.lowerBoundIfFlexible().isMarkedNullable()
////                }
////            }
//
//            return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
//        }
//
//    private fun completeIsSubTypeOf(
//        state: TypeCheckerState,
//        subType: CangJieTypeMarker,
//        superType: CangJieTypeMarker,
//        isFromNullabilityConstraint: Boolean
//    ): Boolean = with(state.typeSystemContext) {
//        val preparedSubType = state.prepareType(state.refineType(subType))
//        val preparedSuperType = state.prepareType(state.refineType(superType))
//
//        checkSubtypeForSpecialCases(state, preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())?.let {
//            state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)
//            return it
//        }
//
//        // we should add constraints with flexible types, otherwise we never get flexible type as answer in constraint system
//        state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)?.let { return it }
//
//        return isSubtypeOfForSingleClassifierType(state, preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())
//    }
//    @JvmOverloads
//    fun isSubtypeOf(
//        state: TypeCheckerState,
//        subType: CangJieTypeMarker,
//        superType: CangJieTypeMarker,
//        isFromNullabilityConstraint: Boolean = false
//    ): Boolean {
//        if (subType === superType) return true
//
////        if (!state.customIsSubtypeOf(subType, superType)) return false
//
//        return completeIsSubTypeOf(state, subType, superType, isFromNullabilityConstraint)
//    }
//
//
//}
