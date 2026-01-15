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

package org.cangnova.cangjie.types.model


import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeChecker
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeInfo
import org.cangnova.cangjie.types.TypeCheckerState
import org.cangnova.cangjie.types.functions.FunctionTypeKind

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// 1. 类型标记接口
interface CangJieTypeMarker
interface TypeArgumentMarker
interface TypeConstructorMarker
interface TypeParameterMarker

// 2. Option相关类型标记
interface SimpleTypeMarker : CangJieTypeMarker
interface CapturedTypeMarker : SimpleTypeMarker

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



interface TypeSystemOptimizationContext {
    /**
     * 判断两个简单类型的类型参数是否完全一致
     * @return true 表示参数完全一致，false 表示不支持或不一致
     */
    fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker) = false
}

/**
 * 类型系统内建类型上下文
 * 提供获取基础类型（如Any、Nothing）的方法
 */
interface TypeSystemBuiltInsContext {
    fun optionNothingType():  SimpleTypeMarker
    fun optionAnyType():  SimpleTypeMarker
    fun nothingType(): SimpleTypeMarker
    fun anyType(): SimpleTypeMarker
}

/**
 * 类型系统类型工厂上下文
 * 提供类型构造、类型参数、Option等相关方法
 */
interface TypeSystemTypeFactoryContext : TypeSystemBuiltInsContext {
    fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): CangJieTypeMarker
    fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,

        isExtensionFunction: Boolean = false,
        attributes: List<AnnotationMarker>? = null
    ): SimpleTypeMarker

    fun createTypeArgument(type: CangJieTypeMarker ): TypeArgumentMarker
    fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): SimpleTypeMarker
    fun createUninferredType(constructor: TypeConstructorMarker): CangJieTypeMarker
}

/**
 * 类型检查器工厂上下文
 * 提供类型检查器的创建方法
 */
interface TypeCheckerProviderContext {
    fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean,
        allowOptionBoxing: Boolean = true
    ): TypeCheckerState
}

/**
 * 类型系统公共超类型上下文
 * 提供超类型计算、Option相关操作等
 */
interface TypeSystemCommonSuperTypesContext : TypeSystemContext, TypeSystemTypeFactoryContext,
    TypeCheckerProviderContext {
    /**
     * 判断类型是否可能为Option类型（如类型参数、捕获类型等）
     */
    fun CangJieTypeMarker.canHaveUndefinedOption(): Boolean
    fun SimpleTypeMarker.isExtensionFunction(): Boolean
    fun SimpleTypeMarker.typeDepth(): Int
    fun CangJieTypeMarker.typeDepth(): Int = when (this) {
        is SimpleTypeMarker -> typeDepth()
        is FlexibleTypeMarker -> maxOf(lowerBound().typeDepth(), upperBound().typeDepth())
        else -> error("类型必须是简单类型或灵活类型: $this")
    }

    fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker?
    fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker
    fun unionTypeAttributes(types: List<CangJieTypeMarker>): List<AnnotationMarker>
    fun CangJieTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): CangJieTypeMarker
}

interface TypeSystemInferenceExtensionContextDelegate : TypeSystemInferenceExtensionContext

/**
 * 类型系统推断扩展上下文
 * 提供类型推断、类型参数消除、Option相关操作等
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

    fun TypeConstructorMarker.isFinalClassConstructor(): Boolean
    fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker
    fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker
    fun CapturedTypeMarker.typeParameter(): TypeParameterMarker?
    fun CapturedTypeMarker.withNonOptionProjection(): CangJieTypeMarker
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
    fun CangJieTypeMarker.isFunctionWithAny(): Boolean
    fun CangJieTypeMarker.functionTypeKind(): FunctionTypeKind?
    fun CangJieTypeMarker.getFunctionTypeFromSupertypes(): CangJieTypeMarker
    fun getNonReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker
    fun getReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker
    fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker

    /**
     * 从当前类型标记中提取特定类型参数，并将其添加到指定集合中
     */
    private fun <T> CangJieTypeMarker.extractTypeOf(to: MutableSet<T>, getIfApplicable: (TypeConstructorMarker) -> T?) {
        for (i in 0 until argumentsCount()) {
            val argument = getArgument(i)
            val argumentType = argument.getType()
            val argumentTypeConstructor = argumentType.typeConstructor()
            val argumentToAdd = getIfApplicable(argumentTypeConstructor)
            if (argumentToAdd != null) {
                to.add(argumentToAdd)
            } else if (argumentType.argumentsCount() != 0) {
                argumentType.extractTypeOf(to, getIfApplicable)
            }
        }
    }

    /**
     * 从当前类型标记中提取类型变量集合
     */
    fun CangJieTypeMarker.extractTypeVariables(): Set<TypeVariableTypeConstructorMarker> =
        buildSet {
            extractTypeOf(this) { it as? TypeVariableTypeConstructorMarker }
        }

    fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker?

    fun CangJieTypeMarker.extractTypeParameters(): Set<TypeParameterMarker> =
        buildSet {
            typeConstructor().getTypeParameterClassifier()?.let(::add)
            extractTypeOf(this) { it.getTypeParameterClassifier() }
        }

    fun useRefinedBoundsForTypeVariableInFlexiblePosition(): Boolean
    fun CangJieTypeMarker.convertToNonRaw(): CangJieTypeMarker
    fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker
    fun createSubstitutorForSuperTypes(baseType: CangJieTypeMarker): TypeSubstitutorMarker?
}

class ArgumentList(initialSize: Int) : ArrayList<TypeArgumentMarker>(initialSize), TypeArgumentListMarker

/**
 * 仓颉类型系统上下文接口
 * 提供类型系统的通用操作，包括类型转换、Option判断、类型参数、类型构造器等
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
    fun CangJieTypeMarker.isDynamic(): Boolean = asFlexibleType()?.asDynamicType() != null
    fun CangJieTypeMarker.isFlexibleNothing() =
        this is FlexibleTypeMarker && lowerBound().isNothing() && upperBound().isOptionNothing()
    fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean

    fun CangJieTypeMarker.isOptionNothing() = this.typeConstructor().isNothingConstructor() && this.isOptionType()

    fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean
    fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean
    fun TypeConstructorMarker.isLocalType(): Boolean
    fun captureFromExpression(type: CangJieTypeMarker): CangJieTypeMarker?
    fun TypeConstructorMarker.isAnonymous(): Boolean
    fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker
    fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker>
    fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker
    fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker
    fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker?
    fun CangJieTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<CangJieTypeMarker>
    fun CangJieTypeMarker.isCapturedType() = asSimpleType()?.asCapturedType() != null
    fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker

    fun CangJieTypeMarker.isNonOptionTypeParameter(): Boolean = false

    fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean
    fun CangJieTypeMarker.upperBoundIfFlexible(): SimpleTypeMarker =
        this.asFlexibleType()?.upperBound() ?: this.asSimpleType()!!
    fun CangJieTypeMarker.isOptionAny() = this.typeConstructor().isAnyConstructor() && this.isOptionType()

    fun TypeConstructorMarker.isIntersection(): Boolean
    fun CangJieTypeMarker.isNothing() = this.typeConstructor().isNothingConstructor() && !this.isOptionType()
    fun CangJieTypeMarker.isOptionType(): Boolean
    val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
    fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker? = null): Boolean

    fun SimpleTypeMarker.isMarkedOption(): Boolean
    fun CangJieTypeMarker.isMarkedOption(): Boolean =
        this is SimpleTypeMarker && isMarkedOption()
    fun TypeParameterMarker.upperBoundCount(): Int

    fun TypeConstructorMarker.isInterface(): Boolean
    fun CangJieTypeMarker.isFlexible(): Boolean = asFlexibleType() != null
    fun CangJieTypeMarker.hasFlexibleOption() =
        lowerBoundIfFlexible().isMarkedOption() != upperBoundIfFlexible().isMarkedOption()

    fun SimpleTypeMarker.withOption(isOption: Boolean): SimpleTypeMarker
    fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker
    fun CangJieTypeMarker.withOption(isOption: Boolean): CangJieTypeMarker
    fun CapturedTypeMarker.isOldCapturedType(): Boolean
    fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker
    fun CapturedTypeMarker.captureStatus(): CaptureStatus
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
    fun TypeArgumentMarker.getType(): CangJieTypeMarker
    fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker
    fun TypeConstructorMarker.parametersCount(): Int
    fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker
    fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker>
    fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker>
    fun SimpleTypeMarker.isClassType(): Boolean = typeConstructor().isClassTypeConstructor()
    fun TypeConstructorMarker.isClassTypeConstructor(): Boolean
    fun TypeConstructorMarker.isFloatLiteralTypeConstructor(): Boolean

    /**
     * 判断类型构造器是否为函数类型构造器
     * 用于实现函数类型的特殊子类型检查（参数逆变，返回值协变）
     */
    fun TypeConstructorMarker.isFunctionTypeConstructor(): Boolean

    /**
     * 判断类型构造器是否为元组类型构造器
     * 用于实现元组类型的严格不变子类型检查（implicitBoxed = false）
     */
    fun TypeConstructorMarker.isTupleTypeConstructor(): Boolean

    fun SimpleTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? = null
    fun SimpleTypeMarker.isFloatLiteralType(): Boolean = typeConstructor().isFloatLiteralTypeConstructor()
    fun SimpleTypeMarker.isIntegerLiteralType(): Boolean = typeConstructor().isIntegerLiteralTypeConstructor()

    /**
     * 判断简单类型是否为函数类型
     * 参考编译器: TypeManager.cpp:870-900 IsFuncSubtype
     */
    fun SimpleTypeMarker.isFunctionType(): Boolean = typeConstructor().isFunctionTypeConstructor()

    /**
     * 判断简单类型是否为元组类型
     * 参考编译器: TypeManager.cpp:902-913 IsTupleSubtype
     */
    fun SimpleTypeMarker.isTupleType(): Boolean = typeConstructor().isTupleTypeConstructor()

    fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean

    fun SimpleTypeMarker.possibleFloatTypes(): Collection<CangJieTypeMarker>
    fun SimpleTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker>
    fun captureFromArguments(
        type: SimpleTypeMarker,
        status: CaptureStatus
    ): SimpleTypeMarker?

    fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker
    operator fun TypeArgumentListMarker.get(index: Int): TypeArgumentMarker {
        return when (this) {
            is SimpleTypeMarker -> getArgument(index)
            is ArgumentList -> get(index)
            else -> error($$"未知类型参数列表类型: $$this, ${this::class}")
        }
    }

    fun CangJieTypeMarker.isCapturedDynamic(): Boolean =
        asSimpleType()?.asCapturedType()?.typeConstructor()?.projection()
            ?.getType()?.isDynamic() == true
    fun TypeArgumentListMarker.size(): Int {
        return when (this) {
            is SimpleTypeMarker -> argumentsCount()
            is ArgumentList -> size
            else -> error($$"未知类型参数列表类型: $$this, ${this::class}")
        }
    }

    fun CangJieTypeMarker.typeConstructor(): TypeConstructorMarker =
        (asSimpleType() ?: lowerBoundIfFlexible()).typeConstructor()

    fun CangJieTypeMarker.lowerBoundIfFlexible(): SimpleTypeMarker =
        this.asFlexibleType()?.lowerBound() ?: this.asSimpleType()!!

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
     * 单一分类类型：包括类类型、类型参数类型、捕获类型
     * 这些类型的类型构造器不是错误类型构造器
     */
    fun SimpleTypeMarker.isSingleClassifierType(): Boolean
    fun intersectTypes(types: List<CangJieTypeMarker>): CangJieTypeMarker
    fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker
    fun CangJieTypeMarker.isSimpleType(): Boolean = asSimpleType() != null
    fun SimpleTypeMarker.isPrimitiveType(): Boolean
    fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker>
    fun substitutionSupertypePolicy(type: SimpleTypeMarker): TypeCheckerState.SupertypesPolicy
    fun CangJieTypeMarker.isTypeVariableType(): Boolean
    fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker
    fun createEmptySubstitutor(): TypeSubstitutorMarker
    fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker


    fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean


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

