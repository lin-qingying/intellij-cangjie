/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls


import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import org.cangnova.cangjie.types.model.*

/**
 * 检索最小公共父类
 */
object CommonSuperTypeCalculator {
    //    fun commonSuperType(types: List<CangJieTypeMarker>): CangJieTypeMarker{
//        return commonSuperType(types)
//    }
    fun TypeSystemCommonSuperTypesContext.commonSuperType(types: List<CangJieTypeMarker>): CangJieTypeMarker {

        val type = commonSuperType(types, true)

        if (type is CangJieType && type.constructor is IntersectionTypeConstructor) {
            return MultipleSupertypeTypeInferenceFailure(type)
        }
        return type.replaceCustomAttributes(unionTypeAttributes(types))
    }

    private fun TypeSystemCommonSuperTypesContext.commonSuperType(
        types: List<CangJieTypeMarker>,
        isTopLevelType: Boolean = false
    ): CangJieTypeMarker {
        if (types.isEmpty()) {
//            throw IllegalStateException("Empty collection for input")
            return ErrorUtils.invalidType
        }

        types.singleOrNull()?.let { return it }

        var thereIsFlexibleTypes = false

        val lowers = types.map {
            when (it) {
                is SimpleTypeMarker -> {
                    if (it.isCapturedDynamic()) return it

                    it
                }

                is FlexibleTypeMarker -> {
                    if (it.isDynamic()) return it
                    // raw types are allowed here and will be transformed to FlexibleTypes

                    thereIsFlexibleTypes = true
                    it.lowerBound()
                }

                else -> error("sealed")
            }
        }

        val stateStubTypesEqualToAnything =
            newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
        val stateStubTypesNotEqual =
            newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false)

        val lowerSuperType =
            commonSuperTypeForSimpleTypes(lowers, stateStubTypesEqualToAnything, stateStubTypesNotEqual)
        if (!thereIsFlexibleTypes) return lowerSuperType

        val upperSuperType = commonSuperTypeForSimpleTypes(
            types.map { it.upperBoundIfFlexible() }, stateStubTypesEqualToAnything, stateStubTypesNotEqual
        )

        if (!isTopLevelType) {
            val nonStubTypes =
                types.filter { !isTypeVariable(it.lowerBoundIfFlexible()) && !isTypeVariable(it.upperBoundIfFlexible()) }
            val equalToEachOtherTypes = nonStubTypes.filter { potentialCommonSuperType ->
                nonStubTypes.all {
                    CangJieTypeChecker.DEFAULT.equalTypes(it as CangJieType, potentialCommonSuperType as CangJieType)
                }
            }

            if (equalToEachOtherTypes.isNotEmpty()) {
                // TODO: merge flexibilities of type arguments instead of select the first suitable type
                return equalToEachOtherTypes.first()
            }
        }

        return createFlexibleType(lowerSuperType, upperSuperType)
    }

    private fun TypeSystemCommonSuperTypesContext.commonSuperTypeForSimpleTypes(
        types: List<SimpleTypeMarker>,

        stateStubTypesEqualToAnything: TypeCheckerState,
        stateStubTypesNotEqual: TypeCheckerState
    ): SimpleTypeMarker {
        if (types.any { it.isError() }) {
            return createErrorType("CST(${types.joinToString()}", delegatedType = null)
        }

        // i.e. result type also should be marked nullable
        val allNotNull = types.all {
            isTypeVariable(it) || isNotNullStubTypeForBuilderInference(it) ||
            // 检查是否是 Any 的子类型(非可空)
            !it.isMarkedOption() && CangJieTypeChecker.DEFAULT.isSubtypeOf(it as CangJieType, anyType() as CangJieType)
        }
        val notNullTypes = if (!allNotNull) types.map { it.withOption(false) } else types

        val commonSuperType =
            commonSuperTypeForNotNullTypes(notNullTypes, stateStubTypesEqualToAnything, stateStubTypesNotEqual)
        return if (!allNotNull)
            refineNullabilityForUndefinedNullability(types, commonSuperType) ?: commonSuperType.withOption(true)
        else
            commonSuperType
    }

    private fun TypeSystemCommonSuperTypesContext.isCapturedStubTypeForVariableInSubtyping(type: SimpleTypeMarker) =
        type.asCapturedType()?.typeConstructor()?.projection()
            ?.getType()?.asSimpleType()?.isStubTypeForVariableInSubtyping() == true

    private fun TypeSystemCommonSuperTypesContext.refineNullabilityForUndefinedNullability(
        types: List<SimpleTypeMarker>,
        commonSuperType: SimpleTypeMarker
    ): SimpleTypeMarker? {
        if (!commonSuperType.canHaveUndefinedOption()) return null

        // 检查是否所有类型到公共超类型都有非可空路径
        val actuallyNotNull = types.all { type ->
            // 简化实现:如果类型不可空且是公共超类型的子类型,则认为有非可空路径
            !type.isMarkedOption()
        }
        return if (actuallyNotNull) commonSuperType else null
    }

    // Makes representative sample, i.e. (A, B, A) -> (A, B)
    private fun TypeSystemCommonSuperTypesContext.uniquify(
        types: List<SimpleTypeMarker>,
        stateStubTypesNotEqual: TypeCheckerState
    ): List<SimpleTypeMarker> {
        val uniqueTypes = arrayListOf<SimpleTypeMarker>()
        for (type in types) {
            val isNewUniqueType = uniqueTypes.all {
                val equalsModuloFlexibility = CangJieTypeChecker.DEFAULT.equalTypes(it as CangJieType, type as CangJieType) &&
                        !it.typeConstructor().isIntegerLiteralTypeConstructor()

                // 仓颉语言的类型系统不需要检查灵活性差异
                !equalsModuloFlexibility
            }
            if (isNewUniqueType) {
                uniqueTypes += type
            }
        }
        return uniqueTypes
    }

    // This function leaves only supertypes, i.e. A0 is a strong supertype for A iff A != A0 && A <: A0
    // Explanation: consider types (A : A0, B : B0, A0, B0), then CST(A, B, A0, B0) == CST(CST(A, A0), CST(B, B0)) == CST(A0, B0)
    private fun TypeSystemCommonSuperTypesContext.filterSupertypes(
        list: List<SimpleTypeMarker>,
        stateStubTypesNotEqual: TypeCheckerState
    ): List<SimpleTypeMarker> {
        val supertypes = list.toMutableList()
        val iterator = supertypes.iterator()
        while (iterator.hasNext()) {
            val potentialSubtype = iterator.next()
            val isSubtype = supertypes.any { supertype ->
                supertype !== potentialSubtype &&
                        CangJieTypeChecker.DEFAULT.isSubtypeOf(potentialSubtype as CangJieType, supertype as CangJieType)
                        // 仓颉语言的类型系统不需要检查灵活性差异
            }

            if (isSubtype) iterator.remove()
        }

        return supertypes
    }

    private fun TypeSystemCommonSuperTypesContext.commonSuperTypeForBuilderInferenceStubTypes(
        stubTypes: List<SimpleTypeMarker>,
        stateStubTypesNotEqual: TypeCheckerState,
    ): SimpleTypeMarker {
        require(stubTypes.isNotEmpty()) { "There should be stub types to compute common super type on them" }

        var areAllDefNotNull = true
        var areThereAnyNullable = false
        val typesToUniquify = buildList {
            for (stubType in stubTypes) {
                when {
                    stubType.isMarkedOption() -> {
                        areThereAnyNullable = true
                        areAllDefNotNull = false
                        add(stubType.withOption(false))
                    }

                    else -> {
                        areAllDefNotNull = false
                        add(stubType)
                    }
                }
            }
        }

        return uniquify(typesToUniquify, stateStubTypesNotEqual).singleOrNull()?.let {
            when {
                areThereAnyNullable -> it.withOption(true)
                else -> it
            }
        } ?: anyType()
    }

    /*
    * Common Supertype calculator works with proper types and stub types (which is a replacement for non-proper types)
    * Also, there are two invariant related to stub types:
    *  - resulting type should be only proper type
    *  - one of the input types is definitely proper type
    * */
    private fun TypeSystemCommonSuperTypesContext.commonSuperTypeForNotNullTypes(
        types: List<SimpleTypeMarker>,

        stateStubTypesEqualToAnything: TypeCheckerState,
        stateStubTypesNotEqual: TypeCheckerState
    ): SimpleTypeMarker {
        if (types.size == 1) return types.single()

        val nonTypeVariables =
            types.filter { !it.isStubTypeForVariableInSubtyping() && !isCapturedStubTypeForVariableInSubtyping(it) }

        assert(nonTypeVariables.isNotEmpty()) {
            "There should be at least one non-stub type to compute common supertype but there are: $types"
        }

        val (builderInferenceStubTypes, nonStubTypes) = nonTypeVariables.partition { it.isStubTypeForBuilderInference() }
        val areAllNonStubTypesNothing =
            nonStubTypes.isNotEmpty() && nonStubTypes.all { it.isNothing() }

        if (builderInferenceStubTypes.isNotEmpty() && (nonStubTypes.isEmpty() || areAllNonStubTypesNothing)) {
            return commonSuperTypeForBuilderInferenceStubTypes(builderInferenceStubTypes, stateStubTypesNotEqual)
        }

        val uniqueTypes = uniquify(nonStubTypes, stateStubTypesNotEqual)
        if (uniqueTypes.size == 1) return uniqueTypes.single()

        val explicitSupertypes = filterSupertypes(uniqueTypes, stateStubTypesNotEqual)
        if (explicitSupertypes.size == 1) return explicitSupertypes.single()
        findErrorTypeInSupertypes(explicitSupertypes, stateStubTypesEqualToAnything)?.let { return it }

        findCommonIntegerLiteralTypesSuperType(explicitSupertypes)?.let { return it }

        return findSuperTypeConstructorsAndIntersectResult(explicitSupertypes, stateStubTypesEqualToAnything)
    }

    private fun TypeSystemCommonSuperTypesContext.isTypeVariable(type: SimpleTypeMarker): Boolean {
        return type.isStubTypeForVariableInSubtyping() || isCapturedTypeVariable(type)
    }

    private fun TypeSystemCommonSuperTypesContext.isNotNullStubTypeForBuilderInference(type: SimpleTypeMarker): Boolean {
        return type.isStubTypeForBuilderInference() && !type.isMarkedOption()
    }

    private fun TypeSystemCommonSuperTypesContext.isCapturedTypeVariable(type: SimpleTypeMarker): Boolean {
        val projectedType =
            type.asCapturedType()?.typeConstructor()?.projection()?.getType()
                ?: return false
        return projectedType.asSimpleType()?.isStubTypeForVariableInSubtyping() == true
    }

    private fun TypeSystemCommonSuperTypesContext.findErrorTypeInSupertypes(
        types: List<SimpleTypeMarker>,
        stateStubTypesEqualToAnything: TypeCheckerState
    ): SimpleTypeMarker? {
        for (type in types) {
            collectAllSupertypes(type, stateStubTypesEqualToAnything).firstOrNull { it.isError() }
                ?.let { return it.toErrorType() }
        }
        return null
    }

    private fun TypeSystemCommonSuperTypesContext.findSuperTypeConstructorsAndIntersectResult(
        types: List<SimpleTypeMarker>,

        stateStubTypesEqualToAnything: TypeCheckerState
    ): SimpleTypeMarker =
        intersectTypes(
            allCommonSuperTypeConstructors(types, stateStubTypesEqualToAnything)
                .mapNotNull { superTypeWithGivenConstructor(types, it) }
        )

    /**
     * Note that if there is captured type C, then no one else is not subtype of C => lowerType cannot help here
     */
    private fun TypeSystemCommonSuperTypesContext.allCommonSuperTypeConstructors(
        types: List<SimpleTypeMarker>,
        stateStubTypesEqualToAnything: TypeCheckerState
    ): List<TypeConstructorMarker> {
        val result = collectAllSupertypes(types.first(), stateStubTypesEqualToAnything)
        // retain all super constructors of the first type that are present in the supertypes of all other types
        for (type in types) {
            if (type === types.first()) continue

            result.retainAll(collectAllSupertypes(type, stateStubTypesEqualToAnything))
        }
        // remove all constructors that have subtype(s) with constructors from the resulting set - they are less precise
        return result.filterNot { target ->
            result.any { other ->
                other != target && other.supertypes().any { it.typeConstructor() == target }
            }
        }
    }

    private fun TypeSystemCommonSuperTypesContext.collectAllSupertypes(
        type: SimpleTypeMarker,
        stateStubTypesEqualToAnything: TypeCheckerState
    ) =
        LinkedHashSet<TypeConstructorMarker>().apply {
            stateStubTypesEqualToAnything.anySupertype(
                type,
                { add(it.typeConstructor()); false },
                { TypeCheckerState.SupertypesPolicy.LowerIfFlexible }
            )
        }

    private fun TypeSystemCommonSuperTypesContext.superTypeWithGivenConstructor(
        types: List<SimpleTypeMarker>,
        constructor: TypeConstructorMarker,

        ): SimpleTypeMarker? {
        if (constructor.parametersCount() == 0) return createSimpleType(
            constructor,
            emptyList()
        )

        val typeCheckerContext = newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)

        /**
         * Sometimes one type can have several supertypes with given type constructor, suppose A <: List<Int> and A <: List<Double>.
         * Also suppose that B <: List<String>.
         * Note that common supertype for A and B is CS(List<Int>, List<String>) & CS(List<Double>, List<String>),
         * but it is too complicated and we will return not so accurate type: CS(List<Int>, List<Double>, List<String>)
         */
        val correspondingSuperTypes = types.flatMap {
            AbstractTypeChecker.findCorrespondingSupertypes(typeCheckerContext, it, constructor)
        }

        val arguments = ArrayList<TypeArgumentMarker>(constructor.parametersCount())
        for (index in 0 until constructor.parametersCount()) {
            val parameter = constructor.getParameter(index)

            val typeProjections = correspondingSuperTypes.mapNotNull {
                val typeArgumentFromSupertype = it.getArgumentOrNull(index) ?: return@mapNotNull null

                // We have to uncapture types with status FOR_SUBTYPING because such captured types are creating during
                // `findCorrespondingSupertypes` call. Normally, we shouldn't create intermediate captured types here, it's needed only
                // to check subtyping. It'll be fixed but for a while we do this uncapturing here
                val typeArgument = uncaptureFromSubtyping(typeArgumentFromSupertype)

                when {


                    typeArgument.getType().lowerBoundIfFlexible().isStubTypeForVariableInSubtyping() -> null

                    else -> typeArgument
                }
            }
//            判断是否所有元素相对
            if (!typeProjections.all { it == typeProjections.first() }) {
                return null
            }
            val argument = calculateArgument(parameter, typeProjections)


            arguments.add(argument)
        }
        return createSimpleType(
            constructor,
            arguments,
            isExtensionFunction = types.all { it.isExtensionFunction() })
    }

    private fun TypeSystemCommonSuperTypesContext.uncaptureFromSubtyping(typeArgument: TypeArgumentMarker): TypeArgumentMarker {
        val capturedType = typeArgument.getType().asSimpleType()?.asCapturedType() ?: return typeArgument
        if (capturedType.captureStatus() != CaptureStatus.FOR_SUBTYPING) return typeArgument

        return capturedType.typeConstructor().projection()
    }

    /**
     * This function returns true in case of detected recursion in type arguments.
     *
     * For situations with self type arguments (or similar ones), the call of this function
     * prevents too deep type argument analysis during super type calculation.
     * Typical examples use something like this interface in hierarchy:
     * ```
     * interface Some<T : Some<T>>
     * ```
     * From point of view of this function we have here something like `Some<CapturedType>` in [originalTypesForCst],
     * and the captured type in argument has the same `Some<CapturedType>` as its constructor supertype.
     *
     * for single super type constructor create star argument argument when types for that argument are equal to the original types.
     * Captured star projections are replaced with their corresponding supertypes during this check.
     * The check is skipped for contravariant parameters, for which recursive cst calculation never happens.
     */
    private fun TypeSystemCommonSuperTypesContext.checkRecursion(
        originalTypesForCst: List<SimpleTypeMarker>,
        typeArgumentsForSuperConstructorParameter: List<TypeArgumentMarker>,
        parameter: TypeParameterMarker,
    ): Boolean {

        val originalTypesSet = originalTypesForCst.toSet()
        val typeArgumentsTypeSet =
            typeArgumentsForSuperConstructorParameter.map { it.getType().lowerBoundIfFlexible() }.toSet()

        if (originalTypesSet.size != typeArgumentsTypeSet.size)
            return false

        // only needed in case of captured star projections in argument types
        val originalTypeConstructorSet by lazy { typeConstructorsWithExpandedStarProjections(originalTypesSet).toSet() }

        for (argumentType in typeArgumentsTypeSet) {
            if (argumentType in originalTypesSet) continue

            var starProjectionFound = false
            for (supertype in supertypesIfCapturedStarProjection(argumentType).orEmpty()) {
                if (supertype.lowerBoundIfFlexible().typeConstructor() !in originalTypeConstructorSet)
                    return false
                else starProjectionFound = true
            }

            if (!starProjectionFound)
                return false
        }
        return true
    }

    private fun TypeSystemCommonSuperTypesContext.typeConstructorsWithExpandedStarProjections(types: Set<SimpleTypeMarker>) =
        sequence {
            for (type in types) {
                if (isCapturedStarProjection(type)) {
                    for (supertype in supertypesIfCapturedStarProjection(type).orEmpty()) {
                        yield(supertype.lowerBoundIfFlexible().typeConstructor())
                    }
                } else {
                    yield(type.typeConstructor())
                }
            }
        }

    private fun TypeSystemCommonSuperTypesContext.isCapturedStarProjection(type: SimpleTypeMarker): Boolean = false
//        type.originalIfDefinitelyNotNullable().asCapturedType()?.typeConstructor()?.argument()
//            ?.isStarProjection() == true

    private fun TypeSystemCommonSuperTypesContext.supertypesIfCapturedStarProjection(type: SimpleTypeMarker): Collection<CangJieTypeMarker>? {
//        val constructor = type.originalIfDefinitelyNotNullable().asCapturedType()?.typeConstructor() ?: return null
//        return if (constructor.argument().isStarProjection())
//            constructor.supertypes()
//        else null
        return null
    }

    // no star projections in arguments
    private fun TypeSystemCommonSuperTypesContext.calculateArgument(
        parameter: TypeParameterMarker,
        arguments: List<TypeArgumentMarker>,

        ): TypeArgumentMarker {


        // Inv<A>, Inv<A> = Inv<A>
        val first = arguments.first()
        if (arguments.all { it.getType() == first.getType() }) return first


        val type = intersectTypes(arguments.map { it.getType() })
        return type.asTypeArgument()

    }
}
