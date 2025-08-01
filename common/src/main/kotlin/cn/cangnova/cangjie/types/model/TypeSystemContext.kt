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

package cn.cangnova.cangjie.types.model


import cn.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeChecker
import cn.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeInfo
import cn.cangnova.cangjie.types.TypeCheckerState
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.functions.FunctionTypeKind

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface CangJieTypeMarker

interface TypeArgumentMarker
interface TypeConstructorMarker
interface TypeParameterMarker

interface SimpleTypeMarker : CangJieTypeMarker
interface CapturedTypeMarker : SimpleTypeMarker
interface DefinitelyNotNullTypeMarker : SimpleTypeMarker

interface FlexibleTypeMarker : CangJieTypeMarker
interface DynamicTypeMarker : FlexibleTypeMarker
interface StubTypeMarker : SimpleTypeMarker

interface TypeArgumentListMarker

interface TypeVariableMarker
interface TypeVariableTypeConstructorMarker : TypeConstructorMarker

interface CapturedTypeConstructorMarker : TypeConstructorMarker

interface IntersectionTypeConstructorMarker : TypeConstructorMarker

interface TypeSubstitutorMarker

interface AnnotationMarker

enum class TypeVariance(val presentation: String) {
//    IN("in"),
//    OUT("out"),
    INV("");

    override fun toString(): String = presentation
}

//fun Variance.convertVariance(): TypeVariance {
//    return when (this) {
//        Variance.INVARIANT -> TypeVariance.INV
//        Variance.IN_VARIANCE -> TypeVariance.IN
//        Variance.OUT_VARIANCE -> TypeVariance.OUT
//    }
//}

interface TypeSystemOptimizationContext {
    /**
     *  @return true is a.arguments == b.arguments, or false if not supported
     */
    fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker) = false
}

/**
 * Context that allow type-impl agnostic access to common types
 */
interface TypeSystemBuiltInsContext {
//    fun nullableNothingType(): SimpleTypeMarker
//    fun nullableAnyType(): SimpleTypeMarker
    fun nothingType(): SimpleTypeMarker
    fun anyType(): SimpleTypeMarker
}

/**
 * Context that allow construction of types
 */
interface TypeSystemTypeFactoryContext : TypeSystemBuiltInsContext {
    fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): CangJieTypeMarker
    fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean = false,
        attributes: List<AnnotationMarker>? = null
    ): SimpleTypeMarker

    fun createTypeArgument(type: CangJieTypeMarker, variance: TypeVariance): TypeArgumentMarker

    fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): SimpleTypeMarker
    fun createUninferredType(constructor: TypeConstructorMarker): CangJieTypeMarker
}

/**
 * Factory, that constructs [cn.cangnova.cangjie.types.TypeCheckerState], which defines type-checker behaviour
 * Implementation is recommended to be [TypeSystemContext]
 */
interface TypeCheckerProviderContext {
    fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState
}

/**
 * Extended type system context, which defines set of operations specific to common super-type calculation
 */
interface TypeSystemCommonSuperTypesContext : TypeSystemContext, TypeSystemTypeFactoryContext,
    TypeCheckerProviderContext {

//    fun CangJieTypeMarker.anySuperTypeConstructor(predicate: (SimpleTypeMarker) -> Boolean) =
//        newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
//            .anySupertype(
//                lowerBoundIfFlexible(),
//                { predicate(it) },
//                { TypeCheckerState.SupertypesPolicy.LowerIfFlexible }
//            )

    fun CangJieTypeMarker.canHaveUndefinedNullability(): Boolean

    fun SimpleTypeMarker.isExtensionFunction(): Boolean

    fun SimpleTypeMarker.typeDepth(): Int

    fun CangJieTypeMarker.typeDepth(): Int = when (this) {
        is SimpleTypeMarker -> typeDepth()
        is FlexibleTypeMarker -> maxOf(lowerBound().typeDepth(), upperBound().typeDepth())
        else -> error("Type should be simple or flexible: $this")
    }

    fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker?

    /*
     * Converts error type constructor to error type
     * Used only in FIR
     */
    fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker

    fun unionTypeAttributes(types: List<CangJieTypeMarker>): List<AnnotationMarker>

    fun CangJieTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): CangJieTypeMarker
}

// This interface is only used to declare that implementing class is supposed to be used as a TypeSystemInferenceExtensionContext component
// Otherwise clash happens during DI container initialization: there are a lot of components that extend TypeSystemInferenceExtensionContext
// but they only has it among supertypes to bring additional receiver into their scopes, i.e. they are not intended to be used as
// component implementation for TypeSystemInferenceExtensionContext
interface TypeSystemInferenceExtensionContextDelegate : TypeSystemInferenceExtensionContext

/**
 * Extended type system context, which defines set of type operations specific for type inference
 */
interface TypeSystemInferenceExtensionContext : TypeSystemContext, TypeSystemBuiltInsContext,
    TypeSystemCommonSuperTypesContext {
    fun CangJieTypeMarker.contains(predicate: (CangJieTypeMarker) -> Boolean): Boolean
    fun computeEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo? =
        EmptyIntersectionTypeChecker.computeEmptyIntersectionEmptiness(this, types)
    fun CangJieTypeMarker.eraseContainingTypeParameters(): CangJieTypeMarker

    fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean
    fun CangJieTypeMarker.hasExactAnnotation(): Boolean = false
    fun CangJieTypeMarker.hasNoInferAnnotation(): Boolean = false

    fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): CangJieTypeMarker

    fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean

//    fun CangJieTypeMarker.eraseContainingTypeParameters(): CangJieTypeMarker

    fun Collection<CangJieTypeMarker>.singleBestRepresentative(): CangJieTypeMarker?

    fun CangJieTypeMarker.isUnit(): Boolean

    fun CangJieTypeMarker.isBuiltinFunctionTypeOrSubtype(): Boolean

    fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<CangJieTypeMarker>,
        lowerType: CangJieTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker

    fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker
    fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker

    fun CangJieTypeMarker.removeAnnotations(): CangJieTypeMarker
//    fun CangJieTypeMarker.removeExactAnnotation(): CangJieTypeMarker

    fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker
    fun SimpleTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleTypeMarker

    fun CangJieTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): CangJieTypeMarker =
        when (this) {
            is SimpleTypeMarker -> replaceArguments(replacement)
            is FlexibleTypeMarker -> createFlexibleType(
                lowerBound().replaceArguments(replacement),
                upperBound().replaceArguments(replacement)
            )

            else -> error("sealed")
        }

    fun SimpleTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleTypeMarker {
        return replaceArguments {

            val type = it.getType()
            val newProjection = if (type.argumentsCount() > 0) {
                it.replaceType(type.replaceArgumentsDeeply(replacement))
            } else it

            replacement(newProjection)
        }
    }

    fun CangJieTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): CangJieTypeMarker {
        return when (this) {
            is SimpleTypeMarker -> replaceArgumentsDeeply(replacement)
            is FlexibleTypeMarker -> createFlexibleType(
                lowerBound().replaceArgumentsDeeply(replacement),
                upperBound().replaceArgumentsDeeply(replacement)
            )

            else -> error("sealed")
        }
    }

//    fun CangJieTypeMarker.hasExactAnnotation(): Boolean
//    fun CangJieTypeMarker.hasNoInferAnnotation(): Boolean

    fun TypeConstructorMarker.isFinalClassConstructor(): Boolean

    fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker

    fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker
    fun CapturedTypeMarker.typeParameter(): TypeParameterMarker?
    fun CapturedTypeMarker.withNotNullProjection(): CangJieTypeMarker


    fun createTypeWithUpperBoundForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker
    ): CangJieTypeMarker


    fun CapturedTypeMarker.hasRawSuperType(): Boolean

    fun TypeVariableMarker.defaultType(): SimpleTypeMarker

    fun createTypeWithAlternativeForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker
    ): CangJieTypeMarker

    fun CangJieTypeMarker.isSpecial(): Boolean

    fun TypeConstructorMarker.isTypeVariable(): Boolean
    fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean

    fun CangJieTypeMarker.isSignedOrUnsignedNumberType(): Boolean

    // ------------- functional type utils -------------

    fun CangJieTypeMarker.isFunctionWithAny(): Boolean

    fun CangJieTypeMarker.functionTypeKind(): FunctionTypeKind?

    fun CangJieTypeMarker.isExtensionFunctionType(): Boolean

//    fun CangJieTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<CangJieTypeMarker>

    fun CangJieTypeMarker.getFunctionTypeFromSupertypes(): CangJieTypeMarker

    fun getNonReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker

    fun getReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker

    // -------------------------------------------------

    fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker

    /**
     * 从当前类型标记中提取特定类型的参数，并将其添加到指定的集合中。
     *
     * @param T 泛型参数，表示要提取的类型。
     * @param to 一个可变集合，用于存储提取的类型。
     * @param getIfApplicable 一个函数，用于根据类型构造器标记获取相应类型的实例。
     *
     * 此函数遍历当前类型标记的所有参数，尝试提取与[TypeConstructorMarker]匹配的类型实例。
     * 如果当前参数直接匹配，则将其转换为指定类型并添加到集合中。
     * 如果当前参数不直接匹配，但包含子参数，则递归调用自身以尝试从子参数中提取匹配的类型。
     */
    private fun <T> CangJieTypeMarker.extractTypeOf(to: MutableSet<T>, getIfApplicable: (TypeConstructorMarker) -> T?) {
        // 遍历当前类型标记的所有参数。
        for (i in 0 until argumentsCount()) {
            val argument = getArgument(i)

            // 获取当前参数的类型和类型构造器。
            val argumentType = argument.getType()
            val argumentTypeConstructor = argumentType.typeConstructor()

            // 尝试使用提供的函数从类型构造器中获取指定类型的实例。
            val argumentToAdd = getIfApplicable(argumentTypeConstructor)

            // 如果成功获取到实例，则添加到指定的集合中。
            if (argumentToAdd != null) {
                to.add(argumentToAdd)
            } else if (argumentType.argumentsCount() != 0) {
                // 如果当前参数不匹配，且包含子参数，则递归调用自身尝试提取子参数中的匹配类型。
                argumentType.extractTypeOf(to, getIfApplicable)
            }
        }
    }

    /**
     * 从当前类型标记中提取类型变量集合
     *
     * 此函数通过遍历当前类型标记中的所有类型，并筛选出其中的类型变量构造器，
     * 最终返回一个包含这些类型变量构造器的集合
     *
     * @return 返回一个集合，包含当前类型标记中所有类型变量类型的构造器
     */
    fun CangJieTypeMarker.extractTypeVariables(): Set<TypeVariableTypeConstructorMarker> =
        buildSet {
            extractTypeOf(this) { it as? TypeVariableTypeConstructorMarker }
        }

    fun CangJieTypeMarker.extractTypeParameters(): Set<TypeParameterMarker> =
        buildSet {
            typeConstructor().getTypeParameterClassifier()?.let(::add)
            extractTypeOf(this) { it.getTypeParameterClassifier() }
        }

    /**
     * For case Foo <: (T..T?) return LowerConstraint for new constraint LowerConstraint <: T
     * In K1, in case nullable it was just Foo?, so constraint was Foo? <: T
     * But it's not 100% correct because prevent having not-nullable upper constraint on T while initial (Foo? <: (T..T?)) is not violated
     *
     * In FIR, we try to have a correct one: (Foo & Any..Foo?) <: T
     *
     * The same logic applies for T! <: UpperConstraint, as well
     * In K1, it was reduced to T <: UpperConstraint..UpperConstraint?
     * In FIR, we use UpperConstraint & Any..UpperConstraint?
     *
     * In future once we have only FIR (or FE 1.0 behavior is fixed) this method should be inlined to the use-site
     */
    fun useRefinedBoundsForTypeVariableInFlexiblePosition(): Boolean

    /**
     * It's only relevant for K2 (and is not expected to be implemented properly in other contexts)
     */
    fun CangJieTypeMarker.convertToNonRaw(): CangJieTypeMarker


    fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker

//    fun createCapturedStarProjectionForSelfType(
//        typeVariable: TypeVariableTypeConstructorMarker,
//        typesForRecursiveTypeParameters: List<CangJieTypeMarker>,
//    ): SimpleTypeMarker? {
//        val typeParameter = typeVariable.typeParameter ?: return null
//
//        val superType = intersectTypes(
//            typesForRecursiveTypeParameters.map { type ->
//                type.replaceArgumentsDeeply {
//                    val constructor = it.getType().typeConstructor()
//                    if (constructor is TypeVariableTypeConstructorMarker && constructor == typeVariable) starProjection else it
//                }
//            }
//        )
//
//        return createCapturedType(TypeProjectionImpl, listOf(superType), lowerType = null, CaptureStatus.FROM_EXPRESSION)
//    }

    fun createSubstitutorForSuperTypes(baseType: CangJieTypeMarker): TypeSubstitutorMarker?

//    fun computeEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo? =
//        EmptyIntersectionTypeChecker.computeEmptyIntersectionEmptiness(this, types)

//    private fun computeEffectiveVariance(parameter: TypeParameterMarker, argument: TypeArgumentMarker): TypeVariance? =
//        AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance())


}


class ArgumentList(initialSize: Int) : ArrayList<TypeArgumentMarker>(initialSize), TypeArgumentListMarker

/**
 * Defines common cangjie type operations with types for abstract types
 */
interface TypeSystemContext : TypeSystemOptimizationContext {
    fun CangJieTypeMarker.asSimpleType(): SimpleTypeMarker?
    fun CangJieTypeMarker.asFlexibleType(): FlexibleTypeMarker?
    fun CangJieTypeMarker.getCustomAttributes(): List<AnnotationMarker>

    fun CangJieTypeMarker.isError(): Boolean
    fun TypeConstructorMarker.isError(): Boolean
    fun CangJieTypeMarker.isUninferredParameter(): Boolean
    fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker?
    fun CangJieTypeMarker.hasCustomAttributes(): Boolean

    fun CangJieTypeMarker.isRawType(): Boolean

    fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker

    fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker
    fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker?
    fun CangJieTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<CangJieTypeMarker>

    fun CangJieTypeMarker.isCapturedType() = asSimpleType()?.asCapturedType() != null

    fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker?
    fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker

    fun SimpleTypeMarker.originalIfDefinitelyNotNullable(): SimpleTypeMarker =
        asDefinitelyNotNullType()?.original() ?: this

    fun CangJieTypeMarker.makeDefinitelyNotNullOrNotNull(): CangJieTypeMarker
    fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker
    fun SimpleTypeMarker.isMarkedNullable(): Boolean
    fun CangJieTypeMarker.isMarkedNullable(): Boolean =
        this is SimpleTypeMarker && isMarkedNullable()

    fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker
    fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker
    fun CangJieTypeMarker.withNullability(nullable: Boolean): CangJieTypeMarker

    fun CapturedTypeMarker.isOldCapturedType(): Boolean
    fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker
    fun CapturedTypeMarker.captureStatus(): CaptureStatus
    fun CapturedTypeMarker.isProjectionNotNull(): Boolean
    fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker

    fun CangJieTypeMarker.argumentsCount(): Int
    fun CangJieTypeMarker.getArgument(index: Int): TypeArgumentMarker
    fun CangJieTypeMarker.getArguments(): List<TypeArgumentMarker>

    fun SimpleTypeMarker.getArgumentOrNull(index: Int): TypeArgumentMarker? {
        if (index in 0 until argumentsCount()) return getArgument(index)
        return null
    }

    fun SimpleTypeMarker.isStubType(): Boolean
    fun SimpleTypeMarker.isStubTypeForVariableInSubtyping(): Boolean
    fun SimpleTypeMarker.isStubTypeForBuilderInference(): Boolean
    fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker

    fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker

    fun CapturedTypeMarker.lowerType(): CangJieTypeMarker?


    fun TypeArgumentMarker.getVariance(): TypeVariance
    fun TypeArgumentMarker.getType(): CangJieTypeMarker
    fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker

    fun TypeConstructorMarker.parametersCount(): Int
    fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker
    fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker>
    fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker>
    fun TypeConstructorMarker.extendSupertypes(): Collection<CangJieTypeMarker>
    fun TypeConstructorMarker.supertypesAndExtend(): Collection<CangJieTypeMarker>

    fun TypeConstructorMarker.isIntersection(): Boolean
    fun TypeConstructorMarker.isClassTypeConstructor(): Boolean
    fun TypeConstructorMarker.isInterface(): Boolean
    fun TypeConstructorMarker.isFloatLiteralTypeConstructor(): Boolean
    fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean
    fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean
    fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean
    fun TypeConstructorMarker.isLocalType(): Boolean
    fun TypeConstructorMarker.isAnonymous(): Boolean
    fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker?
    fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean

    val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?

    fun TypeParameterMarker.getVariance(): TypeVariance
    fun TypeParameterMarker.upperBoundCount(): Int
    fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker
    fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker>
    fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker
    fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker? = null): Boolean

    fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean
    fun captureFromExpression(type: CangJieTypeMarker): CangJieTypeMarker?

    fun TypeConstructorMarker.isDenotable(): Boolean

    fun CangJieTypeMarker.lowerBoundIfFlexible(): SimpleTypeMarker =
        this.asFlexibleType()?.lowerBound() ?: this.asSimpleType()!!

    fun CangJieTypeMarker.upperBoundIfFlexible(): SimpleTypeMarker =
        this.asFlexibleType()?.upperBound() ?: this.asSimpleType()!!

    fun TypeConstructorMarker.isDefinitelyClassTypeConstructor(): Boolean = isClassTypeConstructor() && !isInterface()

    fun CangJieTypeMarker.isFlexible(): Boolean = asFlexibleType() != null

    fun CangJieTypeMarker.isDynamic(): Boolean = asFlexibleType()?.asDynamicType() != null
    fun CangJieTypeMarker.isCapturedDynamic(): Boolean =
        asSimpleType()?.asCapturedType()?.typeConstructor()?.projection()
            ?.getType()?.isDynamic() == true

    fun CangJieTypeMarker.isDefinitelyNotNullType(): Boolean = asSimpleType()?.asDefinitelyNotNullType() != null

    // This kind of types is obsolete (expected to be removed at 1.7) and shouldn't be used further in a new code
    // Now, such types are being replaced with definitely non-nullable types
    @ObsoleteTypeKind
    fun CangJieTypeMarker.isNotNullTypeParameter(): Boolean = false

    fun CangJieTypeMarker.hasFlexibleNullability() =
        lowerBoundIfFlexible().isMarkedNullable() != upperBoundIfFlexible().isMarkedNullable()

    fun CangJieTypeMarker.typeConstructor(): TypeConstructorMarker =
        (asSimpleType() ?: lowerBoundIfFlexible()).typeConstructor()

    fun CangJieTypeMarker.isNullableType(): Boolean

    fun CangJieTypeMarker.isNullableAny() = this.typeConstructor().isAnyConstructor() && this.isNullableType()
    fun CangJieTypeMarker.isNothing() = this.typeConstructor().isNothingConstructor() && !this.isNullableType()
    fun CangJieTypeMarker.isFlexibleNothing() =
        this is FlexibleTypeMarker && lowerBound().isNothing() && upperBound().isNullableNothing()

    fun CangJieTypeMarker.isNullableNothing() = this.typeConstructor().isNothingConstructor() && this.isNullableType()

    fun SimpleTypeMarker.isClassType(): Boolean = typeConstructor().isClassTypeConstructor()

    fun SimpleTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? = null
    fun SimpleTypeMarker.isFloatLiteralType(): Boolean = typeConstructor().isFloatLiteralTypeConstructor()

    fun SimpleTypeMarker.isIntegerLiteralType(): Boolean = typeConstructor().isIntegerLiteralTypeConstructor()
    fun SimpleTypeMarker.possibleFloatTypes(): Collection<CangJieTypeMarker>

    fun SimpleTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker>

//    fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean

    fun captureFromArguments(
        type: SimpleTypeMarker,
        status: CaptureStatus
    ): SimpleTypeMarker?

//    fun captureFromExpression(type: CangJieTypeMarker): CangJieTypeMarker?

    fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker

    operator fun TypeArgumentListMarker.get(index: Int): TypeArgumentMarker {
        return when (this) {
            is SimpleTypeMarker -> getArgument(index)
            is ArgumentList -> get(index)
            else -> error("unknown type argument list type: $this, ${this::class}")
        }
    }

    fun TypeArgumentListMarker.size(): Int {
        return when (this) {
            is SimpleTypeMarker -> argumentsCount()
            is ArgumentList ->  size
            else -> error("unknown type argument list type: $this, ${this::class}")
        }
    }

    operator fun TypeArgumentListMarker.iterator() = object : Iterator<TypeArgumentMarker> {
        private var argumentIndex: Int = 0

        override fun hasNext(): Boolean = argumentIndex < size()

        override fun next(): TypeArgumentMarker {
            val argument = get(argumentIndex)
            argumentIndex += 1
            return argument
        }
    }

    fun TypeConstructorMarker.isAnyConstructor(): Boolean
    fun TypeConstructorMarker.isNothingConstructor(): Boolean
    fun TypeConstructorMarker.isArrayConstructor(): Boolean

    /**
     *
     * SingleClassifierType is one of the following types:
     *  - classType
     *  - type for type parameter
     *  - captured type
     *
     * Such types can contains error types in our arguments, but type constructor isn't errorTypeConstructor
     */
    fun SimpleTypeMarker.isSingleClassifierType(): Boolean

    fun intersectTypes(types: List<CangJieTypeMarker>): CangJieTypeMarker
    fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker

    fun CangJieTypeMarker.isSimpleType(): Boolean = asSimpleType() != null

    fun SimpleTypeMarker.isPrimitiveType(): Boolean

    fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker>

//    fun CangJieTypeMarker.hasCustomAttributes(): Boolean

//    fun CangJieTypeMarker.getCustomAttributes(): List<AnnotationMarker>

    fun substitutionSupertypePolicy(type: SimpleTypeMarker): TypeCheckerState.SupertypesPolicy

    fun CangJieTypeMarker.isTypeVariableType(): Boolean

    fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker
    fun createEmptySubstitutor(): TypeSubstitutorMarker

    /**
     * @returns substituted type or [type] if there were no substitution
     */
    fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker
}

enum class CaptureStatus {
    FOR_SUBTYPING,
    FOR_INCORPORATION,
    FROM_EXPRESSION
}

inline fun TypeArgumentListMarker.all(
    context: TypeSystemContext,
    crossinline predicate: (TypeArgumentMarker) -> Boolean
): Boolean = with(context) {
    repeat(size()) { index ->
        if (!predicate(get(index))) return false
    }
    return true
}

@OptIn(ExperimentalContracts::class)
fun requireOrDescribe(condition: Boolean, value: Any?) {
    contract {
        returns() implies condition
    }
    require(condition) {
        val typeInfo = if (value != null) {
            ", type = '${value::class}'"
        } else ""
        "Unexpected: value = '$value'$typeInfo"
    }
}

@RequiresOptIn("This kinds of type is obsolete and should not be used until you really need it")
annotation class ObsoleteTypeKind



fun Variance.convertVariance(): TypeVariance {
    return when (this) {
        Variance.INVARIANT -> TypeVariance.INV
//        Variance.IN_VARIANCE -> TypeVariance.IN
//        Variance.OUT_VARIANCE -> TypeVariance.OUT
    }
}
