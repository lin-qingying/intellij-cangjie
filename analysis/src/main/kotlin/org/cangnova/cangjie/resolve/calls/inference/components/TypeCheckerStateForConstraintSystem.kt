/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.types.AbstractNullabilityChecker
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.AbstractTypePreparator
import org.cangnova.cangjie.types.AbstractTypeRefiner
import org.cangnova.cangjie.types.TypeCheckerState
import org.cangnova.cangjie.types.model.*

abstract class TypeCheckerStateForConstraintSystem(
    val extensionTypeContext: TypeSystemInferenceExtensionContext,
    cangjieTypePreparator: AbstractTypePreparator,
    cangjieTypeRefiner: AbstractTypeRefiner
) : TypeCheckerState(
    isErrorTypeEqualsToAnything = true,
    isStubTypeEqualsToAnything = true,
    allowedTypeVariable = false,
    typeSystemContext = extensionTypeContext,
    cangjieTypePreparator,
    cangjieTypeRefiner
) {
    abstract fun addLowerConstraint(
        typeVariable: TypeConstructorMarker,
        subType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    )

    abstract val isInferenceCompatibilityEnabled: Boolean

    // super and sub type isSingleClassifierType
    abstract fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: CangJieTypeMarker)
    abstract fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: CangJieTypeMarker)

    abstract fun isMyTypeVariable(type: SimpleTypeMarker): Boolean

    private fun CangJieTypeMarker.isTypeVariableWithExact() =
        with(extensionTypeContext) { hasExactAnnotation() } && anyBound(this@TypeCheckerStateForConstraintSystem::isMyTypeVariable)

    private fun CangJieTypeMarker.isTypeVariableWithNoInfer() =
        with(extensionTypeContext) { hasNoInferAnnotation() } && anyBound(this@TypeCheckerStateForConstraintSystem::isMyTypeVariable)

    private inline fun CangJieTypeMarker.anyBound(f: (SimpleTypeMarker) -> Boolean) = when (this) {
        is SimpleTypeMarker -> f(this)
        is FlexibleTypeMarker -> with(typeSystemContext) { f(lowerBound()) || f(upperBound()) }
        else -> error("sealed")
    }

    /**
     * todo: possible we should override this method, because otherwise OR in subtyping transformed to AND in constraint system
     * Now we cannot do this, because sometimes we have proper intersection type as lower type and if we first supertype,
     * then we can get wrong result.
     * override val sameConstructorPolicy get() = SeveralSupertypesWithSameConstructorPolicy.TAKE_FIRST_FOR_SUBTYPING
     */
    final override fun addSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean? {
        val hasNoInfer = subType.isTypeVariableWithNoInfer() || superType.isTypeVariableWithNoInfer()
        if (hasNoInfer) return true

        val hasExact = subType.isTypeVariableWithExact() || superType.isTypeVariableWithExact()

        // we should strip annotation's because we have incorporation operation and they should be not affected
//        val mySubType =
//            if (hasExact) extractTypeForProjectedType(subType, out = true)
//                ?: with(extensionTypeContext) { subType.removeExactAnnotation() } else subType
//        val mySuperType =
//            if (hasExact) extractTypeForProjectedType(superType, out = false)
//                ?: with(extensionTypeContext) { superType.removeExactAnnotation() } else superType

        val result = internalAddSubtypeConstraint(subType, superType, isFromNullabilityConstraint)
        if (!hasExact) return result

        val result2 = internalAddSubtypeConstraint(subType, superType, isFromNullabilityConstraint)

        if (result == null && result2 == null) return null
        return (result ?: true) && (result2 ?: true)
    }

    private fun assertInputTypes(subType: CangJieTypeMarker, superType: CangJieTypeMarker) = with(typeSystemContext) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        fun correctSubType(subType: SimpleTypeMarker) =
            subType.isSingleClassifierType() || subType.typeConstructor()
                .isIntersection() || isMyTypeVariable(subType) || subType.isError() || subType.isIntegerLiteralType()

        fun correctSuperType(superType: SimpleTypeMarker) =
            superType.isSingleClassifierType() || superType.typeConstructor()
                .isIntersection() || isMyTypeVariable(superType) || superType.isError() || superType.isIntegerLiteralType()

        assert(subType.bothBounds(::correctSubType)) {
            "Not singleClassifierType and not intersection subType: $subType"
        }
        assert(superType.bothBounds(::correctSuperType)) {
            "Not singleClassifierType superType: $superType"
        }
    }

    private inline fun CangJieTypeMarker.bothBounds(f: (SimpleTypeMarker) -> Boolean) = when (this) {
        is SimpleTypeMarker -> f(this)
        is FlexibleTypeMarker -> with(typeSystemContext) { f(lowerBound()) && f(upperBound()) }
        else -> error("sealed")
    }

    /**
     * Foo <: T -- leave as is
     *
     * T?
     *
     * Foo <: T? -- Foo & Any <: T
     * Foo? <: T? -- Foo? & Any <: T -- Foo & Any <: T
     * (Foo..Bar) <: T? -- (Foo..Bar) & Any <: T
     *
     * T!
     *
     * Foo <: T! --
     * assert T! == (T..T?)
     *  Foo <: T?
     *  Foo <: T (optional constraint, needs to preserve nullability)
     * =>
     *  Foo & Any <: T
     *  Foo <: T
     * =>
     *  (Foo & Any .. Foo) <: T -- (Foo!! .. Foo) <: T
     *
     * => Foo <: T! -- (Foo!! .. Foo) <: T
     *
     * Foo? <: T! -- Foo? <: T
     *
     *
     * (Foo..Bar) <: T! --
     * assert T! == (T..T?)
     *  (Foo..Bar) <: (T..T?)
     * =>
     *  Foo <: T?
     *  Bar <: T (optional constraint, needs to preserve nullability)
     * =>
     *  (Foo & Any .. Bar) <: T -- (Foo!! .. Bar) <: T
     *
     * => (Foo..Bar) <: T! -- (Foo!! .. Bar) <: T
     *
     * T & Any
     *
     * Foo? <: T & Any => ERROR (for K2 only)
     *
     * Foo..Bar? <: T & Any => Foo..Bar? <: T
     * Foo <: T & Any  => Foo <: T
     */
    private fun simplifyLowerConstraint(
        typeVariable: CangJieTypeMarker,
        subType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean = with(extensionTypeContext) {
        val subTypeConstructor = subType.typeConstructor()
        val lowerConstraint = when (typeVariable) {
            is SimpleTypeMarker ->
                when {
                    // Foo? (any type which cannot be used as dispatch receiver because of nullability) <: T & Any => ERROR (for K2 only)
                    typeVariable.isDefinitelyNonOptionType() && !subTypeConstructor.isTypeVariable() &&
                            !AbstractNullabilityChecker.isSubtypeOfAny(extensionTypeContext, subType) -> {
                        return false
                    }
                    /*
                     * Foo <: T? (T is contained in invariant or contravariant positions of a return type) -- Foo <: T
                     *      Example:
                     *          fun <T> foo(x: T?): Inv<T> {}
                     *          fun <K> main(z: K) { val x = foo(z) }
                     * Foo <: T? (T isn't contained there) -- Foo!! <: T
                     *      Example:
                     *          fun <T> foo(x: T?) {}
                     *          fun <K> main(z: K) { foo(z) }
                    */
                    typeVariable.isMarkedOption() -> {
                        val typeVariableTypeConstructor = typeVariable.typeConstructor()
                        val needToMakeDefNotNull = subTypeConstructor.isTypeVariable() ||
                                typeVariableTypeConstructor !is TypeVariableTypeConstructorMarker ||
                                !typeVariableTypeConstructor.isContainedInInvariantOrContravariantPositions()

                        val resultType = if (needToMakeDefNotNull) {
                            subType.makeDefinitelyNonOptionOrNonOption()
                        } else {
                            if (!isInferenceCompatibilityEnabled && subType is CapturedTypeMarker) {
                                subType.withNonOptionProjection()
                            } else {
                                subType.withOption(false)
                            }
                        }
                        if (isInferenceCompatibilityEnabled && resultType is CapturedTypeMarker) resultType.withNonOptionProjection() else resultType
                    }
                    // Foo <: T => Foo <: T
                    else -> subType
                }

            is FlexibleTypeMarker -> {
                assertFlexibleTypeVariable(typeVariable)

                when (subType) {
                    is SimpleTypeMarker ->
                        when {
                            useRefinedBoundsForTypeVariableInFlexiblePosition() ->
                                // Foo <: T! -- (Foo!! .. Foo) <: T
                                createFlexibleType(
                                    subType.makeSimpleTypeDefinitelyNonOptionOrNonOption(),
                                    subType.withOption(true)
                                )
                            // In K1 (FE1.0), there is an obsolete behavior
                            subType.isMarkedOption() -> subType
                            else -> createFlexibleType(subType, subType.withOption(true))
                        }

                    is FlexibleTypeMarker ->

                        when {
                            useRefinedBoundsForTypeVariableInFlexiblePosition() ->
                                // (Foo..Bar) <: T! -- (Foo!! .. Bar?) <: T
                                createFlexibleType(
                                    subType.lowerBound().makeSimpleTypeDefinitelyNonOptionOrNonOption(),
                                    subType.upperBound().withOption(true)
                                )

                            else ->
                                // (Foo..Bar) <: T! -- (Foo!! .. Bar) <: T
                                createFlexibleType(
                                    subType.lowerBound().makeSimpleTypeDefinitelyNonOptionOrNonOption(),
                                    subType.upperBound()
                                )
                        }

                    else -> error("sealed")
                }
            }

            else -> error("sealed")
        }

        addLowerConstraint(typeVariable.typeConstructor(), lowerConstraint, isFromNullabilityConstraint)

        return true
    }

    private fun assertFlexibleTypeVariable(typeVariable: FlexibleTypeMarker) = with(typeSystemContext) {
        assert(typeVariable.lowerBound().typeConstructor() == typeVariable.upperBound().typeConstructor()) {
            "Flexible type variable ($typeVariable) should have bounds with the same type constructor, i.e. (T..T?)"
        }
    }

    private fun internalAddSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean? {
        assertInputTypes(subType, superType)

        var answer: Boolean? = null

        if (superType.anyBound(this::isMyTypeVariable)) {
            answer = simplifyLowerConstraint(superType, subType, isFromNullabilityConstraint)
        }

        if (subType.anyBound(this::isMyTypeVariable)) {
            return simplifyUpperConstraint(subType, superType) && (answer ?: true)
        } else {
            extractTypeVariableForSubtype(subType, superType)?.let {
                return simplifyUpperConstraint(it, superType) && (answer ?: true)
            }

            return simplifyConstraintForPossibleIntersectionSubType(subType, superType) ?: answer
        }
    }

    private fun extractTypeVariableForSubtype(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker
    ): CangJieTypeMarker? =
        with(extensionTypeContext) {

            val typeMarker = subType.asSimpleType()?.asCapturedType() ?: return null

            val projection = typeMarker.typeConstructorProjection()

            return null

        }

    /**
     * T! <: Foo <=> T <: Foo & Any..Foo?
     * T? <: Foo <=> T <: Foo && Nothing? <: Foo
     * T  <: Foo -- leave as is
     * T & Any <: Foo <=> T <: Foo?
     */
    private fun simplifyUpperConstraint(typeVariable: CangJieTypeMarker, superType: CangJieTypeMarker): Boolean =
        with(extensionTypeContext) {
            val typeVariableLowerBound = typeVariable.lowerBoundIfFlexible()

            val simplifiedSuperType = when {
                typeVariable.isFlexible() && useRefinedBoundsForTypeVariableInFlexiblePosition() ->
                    createFlexibleType(
                        superType.lowerBoundIfFlexible().makeSimpleTypeDefinitelyNonOptionOrNonOption(),
                        superType.upperBoundIfFlexible().withOption(true)
                    )

                typeVariableLowerBound.isDefinitelyNonOptionType() -> {
                    superType.withOption(true)
                }

                typeVariable.isFlexible() && superType is SimpleTypeMarker ->
                    createFlexibleType(superType, superType.withOption(true))

                else -> superType
            }

            addUpperConstraint(typeVariableLowerBound.typeConstructor(), simplifiedSuperType)

            if (typeVariableLowerBound.isMarkedOption()) {
                // here is important that superType is singleClassifierType
                return simplifiedSuperType.anyBound(::isMyTypeVariable) ||
                        isSubtypeOfByTypeChecker(nothingType(), simplifiedSuperType)
            }

            return true
        }

    private fun isSubtypeOfByTypeChecker(subType: CangJieTypeMarker, superType: CangJieTypeMarker) =
        AbstractTypeChecker.isSubtypeOf(this as TypeCheckerState, subType, superType)

    private fun simplifyConstraintForPossibleIntersectionSubType(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker
    ): Boolean? =
        with(extensionTypeContext) {
            @Suppress("NAME_SHADOWING")
            val subType = subType.lowerBoundIfFlexible()

            if (!subType.typeConstructor().isIntersection()) return null

            assert(!subType.isMarkedOption()) { "Intersection type should not be marked nullable!: $subType" }

            // TODO: may be we lose flexibility here
            val subIntersectionTypes = (subType.typeConstructor().supertypes()).map { it.lowerBoundIfFlexible() }

            val typeVariables =
                subIntersectionTypes.filter(::isMyTypeVariable).takeIf { it.isNotEmpty() } ?: return null
            val notTypeVariables = subIntersectionTypes.filterNot(::isMyTypeVariable)

            // todo: may be we can do better then that.
            if (notTypeVariables.isNotEmpty() &&
                AbstractTypeChecker.isSubtypeOf(
                    this as TypeCheckerProviderContext,
                    intersectTypes(notTypeVariables),
                    superType
                )
            ) {
                return true
            }

//       Consider the following example:
//      fun <T> id(x: T): T = x
//      fun <S> id2(x: S?, y: S): S = y
//
//      fun checkLeftAssoc(a: Int?) : Int {
//          return id2(id(a), 3)
//      }
//
//      fun box() : String {
//          return "OK"
//      }
//
//      here we try to add constraint {Any & T} <: S from `id(a)`
//      Previously we thought that if `Any` isn't a subtype of S => T <: S, which is wrong, now we use weaker upper constraint
//      TODO: rethink, maybe we should take nullability into account somewhere else
            if (notTypeVariables.any {
                    AbstractNullabilityChecker.isSubtypeOfAny(
                        this as TypeCheckerProviderContext,
                        it
                    )
                }) {
                return typeVariables.all { simplifyUpperConstraint(it, superType.withOption(true)) }
            }

            return typeVariables.all { simplifyUpperConstraint(it, superType) }
        }

}
