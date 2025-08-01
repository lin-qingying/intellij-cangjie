/*
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
package cn.cangnova.cangjie.types.checker

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.EnrichedProjectionKind
import cn.cangnova.cangjie.types.TypeProjection
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.util.TypeUtils

class TypeCheckingProcedure(private val constraints: TypeCheckingProcedureCallbacks) {
    fun equalsIgnoringGenerics(type1: CangJieType, type2: CangJieType): Boolean {
        if (type1 === type2) return true
        if (type1.isFlexible()) {
            if (type2.isFlexible()) {
                return !type1.isError && !type2.isError &&
                        isSubtypeOf(type1, type2) && isSubtypeOf(type2, type1)
            }
            return heterogeneousEquivalence(type2, type1)
        } else if (type2.isFlexible()) {
            return heterogeneousEquivalence(type1, type2)
        }

        if (type1.isMarkedOption != type2.isMarkedOption) {
            return false
        }

        if (type1.isMarkedOption) {
            // Then type2 is nullable, too (see the previous condition
            return constraints.assertEqualTypes(
                TypeUtils.makeNotNullable(type1),
                TypeUtils.makeNotNullable(type2),
                this
            )
        }

        val constructor1 = type1.constructor
        val constructor2 = type2.constructor

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false
        }

        val type1Arguments: MutableList<TypeProjection> = type1.arguments
        val type2Arguments: MutableList<TypeProjection> = type2.arguments
        if (type1Arguments.size != type2Arguments.size) {
            return false
        }

        for (i in type1Arguments.indices) {
            val typeProjection1 = type1Arguments.get(i)
            val typeProjection2 = type2Arguments.get(i)

            val typeParameter1 = constructor1.parameters.get(i)
            val typeParameter2 = constructor2.parameters.get(i)

            if (capture(typeProjection1, typeProjection2, typeParameter1)) {
                continue
            }
            if (getEffectiveProjectionKind(
                    typeParameter1,
                    typeProjection1
                ) != getEffectiveProjectionKind(typeParameter2, typeProjection2)
            ) {
                return false
            }

            if (!constraints.assertEqualTypes(typeProjection1.type, typeProjection2.type, this)) {
                return false
            }
        }
        return true
    }

    fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean {
        if (sameTypeConstructors(subtype, supertype)) {
            return !subtype.isMarkedOption || supertype.isMarkedOption
        }
        val subtypeRepresentative: CangJieType = subtype.getSubtypeRepresentative()
        val supertypeRepresentative: CangJieType = supertype.getSupertypeRepresentative()
        if (subtypeRepresentative !== subtype || supertypeRepresentative !== supertype) {
            // recursive invocation for possible chain of representatives
            return isSubtypeOf(subtypeRepresentative, supertypeRepresentative)
        }
        return isSubtypeOfForRepresentatives(subtype, supertype)
    }

    protected fun heterogeneousEquivalence(inflexibleType: CangJieType, flexibleType: CangJieType): Boolean {
        // This is to account for the case when we have Collection<X> vs (Mutable)Collection<X>! or K(java.util.Collection<? extends X>)
        assert(!inflexibleType.isFlexible()) { "Only inflexible types are allowed here: " + inflexibleType }
        return isSubtypeOf(flexibleType.asFlexibleType().lowerBound, inflexibleType)
                && isSubtypeOf(inflexibleType, flexibleType.asFlexibleType().upperBound)
    }

    fun equalTypes(type1: CangJieType, type2: CangJieType): Boolean {
        if (type1 === type2) return true
        if (type1.isFlexible()) {
            if (type2.isFlexible()) {
                return !type1.isError && !type2.isError &&
                        isSubtypeOf(type1, type2) && isSubtypeOf(type2, type1)
            }
            return heterogeneousEquivalence(type2, type1)
        } else if (type2.isFlexible()) {
            return heterogeneousEquivalence(type1, type2)
        }

        if (type1.isMarkedOption != type2.isMarkedOption) {
            return false
        }

        if (type1.isMarkedOption) {
            // Then type2 is nullable, too (see the previous condition
            return constraints.assertEqualTypes(
                TypeUtils.makeNotNullable(type1),
                TypeUtils.makeNotNullable(type2),
                this
            )
        }

        val constructor1 = type1.constructor
        val constructor2 = type2.constructor

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false
        }

        val type1Arguments: MutableList<TypeProjection> = type1.arguments
        val type2Arguments: MutableList<TypeProjection> = type2.arguments
        if (type1Arguments.size != type2Arguments.size) {
            return false
        }

        for (i in type1Arguments.indices) {
            val typeProjection1 = type1Arguments.get(i)
            val typeProjection2 = type2Arguments.get(i)

            val typeParameter1 = constructor1.parameters.get(i)
            val typeParameter2 = constructor2.parameters.get(i)

            if (capture(typeProjection1, typeProjection2, typeParameter1)) {
                continue
            }
            if (getEffectiveProjectionKind(
                    typeParameter1,
                    typeProjection1
                ) != getEffectiveProjectionKind(typeParameter2, typeProjection2)
            ) {
                return false
            }

            if (!constraints.assertEqualTypes(typeProjection1.type, typeProjection2.type, this)) {
                return false
            }
        }
        return true
    }

    private fun capture(
        subtypeArgumentProjection: TypeProjection,
        supertypeArgumentProjection: TypeProjection,
        parameter: TypeParameterDescriptor
    ): Boolean {
        // Capturing makes sense only for invariant classes
        if (parameter.variance !== Variance.INVARIANT) return false

        // Now, both subtype and supertype relations transform to equality constraints on type arguments:
        // Array<out Int> is a subtype or equal to Array<T> then T captures a type that extends Int: 'Captured(out Int)'
        // Array<in Int> is a subtype or equal to Array<T> then T captures a type that extends Int: 'Captured(in Int)'
        if (subtypeArgumentProjection.projectionKind !== Variance.INVARIANT && supertypeArgumentProjection.projectionKind === Variance.INVARIANT) {
            return constraints.capture(supertypeArgumentProjection.type, subtypeArgumentProjection)
        }
        return false
    }

    private fun isSubtypeOfForRepresentatives(subtype: CangJieType, supertype: CangJieType): Boolean {
        if (subtype.isError || supertype.isError) {
            return true
        }

        if (!supertype.isMarkedOption && subtype.isMarkedOption) {
            return false
        }

        if (CangJieBuiltIns.isNothing(subtype)) {
            return true
        }

        val closestSupertype: CangJieType? = findCorrespondingSupertype(subtype, supertype, constraints)
        if (closestSupertype == null) {
            return constraints.noCorrespondingSupertype(
                subtype,
                supertype
            ) // if this returns true, there still isn't any supertype to continue with
        }

        if (!supertype.isMarkedOption && closestSupertype.isMarkedOption) {
            return false
        }

        return checkSubtypeForTheSameConstructor(closestSupertype, supertype)
    }

    private fun checkSubtypeForTheSameConstructor(subtype: CangJieType, supertype: CangJieType): Boolean {
        val constructor = subtype.constructor

        // this assert was moved to checker/utils.cj
        //assert constraints.assertEqualTypeConstructors(constructor, supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();
        val subArguments: MutableList<TypeProjection> = subtype.arguments
        val superArguments: MutableList<TypeProjection> = supertype.arguments
        if (subArguments.size != superArguments.size) return false

        val parameters: MutableList<TypeParameterDescriptor> = constructor.parameters
        for (i in parameters.indices) {
            val parameter = parameters.get(i)

            val superArgument = superArguments.get(i)
            val subArgument = subArguments.get(i)



            if (capture(subArgument, superArgument, parameter)) continue

            val argumentIsErrorType = subArgument.type.isError || superArgument.type.isError
            if (!argumentIsErrorType && parameter.variance === Variance.INVARIANT && subArgument.projectionKind === Variance.INVARIANT && superArgument.projectionKind === Variance.INVARIANT) {
                if (!constraints.assertEqualTypes(subArgument.type, superArgument.type, this)) return false
                continue
            }

            val superOut: CangJieType = getOutType(parameter, superArgument)
            val subOut: CangJieType = getOutType(parameter, subArgument)
            if (!constraints.assertSubtype(subOut, superOut, this)) return false
        }
        return true
    }

    companion object {
        // This method returns the supertype of the first parameter that has the same constructor
        // as the second parameter, applying the substitution of type arguments to it
        // This method returns the supertype of the first parameter that has the same constructor
        // as the second parameter, applying the substitution of type arguments to it
        @JvmOverloads
        fun findCorrespondingSupertype(
            subtype: CangJieType,
            supertype: CangJieType,
            typeCheckingProcedureCallbacks: TypeCheckingProcedureCallbacks = TypeCheckerProcedureCallbacksImpl()
        ): CangJieType? {
            return findCorrespondingSupertype(subtype, supertype, typeCheckingProcedureCallbacks)
        }

        // If class C<out T> then C<T> and C<out T> mean the same
        // out * out = out
        // out * in  = *
        // out * inv = out
        //
        // in * out  = *
        // in * in   = in
        // in * inv  = in
        //
        // inv * out = out
        // inv * in  = out
        // inv * inv = inv
        fun getEffectiveProjectionKind(
            typeParameterVariance: Variance,
            typeArgumentVariance: Variance
        ): EnrichedProjectionKind {
            return EnrichedProjectionKind.Companion.getEffectiveProjectionKind(
                typeParameterVariance,
                typeArgumentVariance
            )
        }

        fun getEffectiveProjectionKind(
            typeParameter: TypeParameterDescriptor,
            typeArgument: TypeProjection
        ): EnrichedProjectionKind? {
            return getEffectiveProjectionKind(typeParameter.variance, typeArgument.projectionKind)
        }


        private fun getOutType(parameter: TypeParameterDescriptor, argument: TypeProjection): CangJieType {
            return parameter.builtIns.getAnyType()
        }
    }
}
