package cn.cangnova.cangjie.types.checker/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

//package cn.cangnova.cangjie.types.checker
//
//import cn.cangnova.cangjie.types.CangJieTypeMarker
//import cn.cangnova.cangjie.types.TypeCheckerState
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
////                    return simpleA.isMarkedOption() == refinedB.lowerBoundIfFlexible().isMarkedOption()
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
