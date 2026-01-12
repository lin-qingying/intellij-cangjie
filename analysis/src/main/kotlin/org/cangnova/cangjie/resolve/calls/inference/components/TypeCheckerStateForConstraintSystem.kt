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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.types.AbstractOptionChecker
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.AbstractTypePreparator
import org.cangnova.cangjie.types.AbstractTypeRefiner
import org.cangnova.cangjie.types.TypeCheckerState
import org.cangnova.cangjie.types.model.*

/**
 * 仓颉语言类型检查器状态 - 用于约束系统
 *
 * 仓颉语言特性：
 * - 基于 LLVM，不与 Java 互操作
 * - 所有用户定义的泛型类型参数都是不变的 (invariant)
 * - 使用 Option 类型 (?T) 而非 Kotlin 的 nullable
 * - **Option 是确切的类型，不能被改变或操作**
 * - 没有星投影 (star projection)、in/out 投影的概念
 * - 没有 DNN (Definitely Not Null) 类型
 * - 没有可空性 (nullability) 概念
 */
abstract class TypeCheckerStateForConstraintSystem(
    val extensionTypeContext: TypeSystemInferenceExtensionContext,
    cangjieTypePreparator: AbstractTypePreparator,
    cangjieTypeRefiner: AbstractTypeRefiner
) : TypeCheckerState(
    isErrorTypeEqualsToAnything = true,
    isStubTypeEqualsToAnything = true,
    isDnnTypesEqualToFlexible = false,
    allowedTypeVariable = false,
    typeSystemContext = extensionTypeContext,
    cangjieTypePreparator,
    cangjieTypeRefiner
) {
    abstract val languageVersionSettings: LanguageVersionSettings

    abstract fun isMyTypeVariable(type: SimpleTypeMarker): Boolean

    // super and sub type isSingleClassifierType
    abstract fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: CangJieTypeMarker, isNoInfer: Boolean)

    abstract fun addLowerConstraint(
        typeVariable: TypeConstructorMarker,
        subType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false,
        isNoInfer: Boolean,
    )

    abstract fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: CangJieTypeMarker)

    override fun getLowerCapturedTypePolicy(subType: SimpleTypeMarker, superType: CapturedTypeMarker): LowerCapturedTypePolicy =
        with(extensionTypeContext) {
            return when {
                isMyTypeVariable(subType) -> {
                    val projection = superType.typeConstructorProjection()
                    val type = projection.getType()?.asSimpleType()
                    // 仓颉语言：所有泛型都是不变的，没有 IN/OUT 变型
                    // 简化逻辑：如果是类型变量，检查下界；否则跳过
                    if (type != null && isMyTypeVariable(type)) {
                        LowerCapturedTypePolicy.CHECK_ONLY_LOWER
                    } else {
                        LowerCapturedTypePolicy.SKIP_LOWER
                    }
                }
                subType.contains { it.anyBound(::isMyTypeVariable) } -> LowerCapturedTypePolicy.CHECK_ONLY_LOWER
                else -> LowerCapturedTypePolicy.CHECK_SUBTYPE_AND_LOWER
            }
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
        val subTypeHasNoInfer = subType.isTypeVariableWithNoInfer()
        val superTypeHasNoInfer = superType.isTypeVariableWithNoInfer()
        val isNoInfer = subTypeHasNoInfer || superTypeHasNoInfer
        if (isNoInfer) {
            return true
        }

        val hasExact = subType.isTypeVariableWithExact() || superType.isTypeVariableWithExact()

        // 仓颉语言：移除 Exact 注解处理，因为仓颉不支持注解式类型修饰
        // 直接处理类型，不移除任何注解
        val mySubType = if (hasExact) extractTypeForProjectedType(subType, out = true) ?: subType else subType
        val mySuperType = if (hasExact) extractTypeForProjectedType(superType, out = false) ?: superType else superType

        val result = internalAddSubtypeConstraint(mySubType, mySuperType, isFromNullabilityConstraint, isNoInfer)
        if (!hasExact) return result

        val result2 = internalAddSubtypeConstraint(mySuperType, mySubType, isFromNullabilityConstraint, isNoInfer)

        if (result == null && result2 == null) return null
        return (result ?: true) && (result2 ?: true)
    }

    private fun extractTypeForProjectedType(type: CangJieTypeMarker, out: Boolean): CangJieTypeMarker? = with(extensionTypeContext) {
        val simpleType = type.asSimpleType()
        val typeMarker = simpleType?.asCapturedType() ?: return null

        val projection = typeMarker.typeConstructorProjection()

        // 仓颉语言：没有星投影概念，getType() 返回 null 表示未指定类型参数
        val projectionType = projection.getType() ?: return when (out) {
            true -> simpleType.typeConstructor().supertypes().let {
                if (it.isEmpty())
                   optionAnyType()
                else
                    intersectTypes(it.toList())
            }
            false -> typeMarker.lowerType()
        }

        // 仓颉语言：所有泛型都是不变的，没有 IN/OUT/INV 变型
        // 简化逻辑：直接返回投影类型
        return projectionType
    }

    private fun CangJieTypeMarker.isTypeVariableWithExact() =
        with(extensionTypeContext) { hasExactAnnotation() } && anyBound(this@TypeCheckerStateForConstraintSystem::isMyTypeVariable)

    private fun CangJieTypeMarker.isTypeVariableWithNoInfer() =
        with(extensionTypeContext) { hasNoInferAnnotation() } && anyBound(this@TypeCheckerStateForConstraintSystem::isMyTypeVariable)

    private fun internalAddSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean,
        isNoInfer: Boolean,
    ): Boolean? {
        assertInputTypes(subType, superType)

        var answer: Boolean? = null

        if (superType.anyBound(this::isMyTypeVariable)) {
            answer = simplifyLowerConstraint(superType, subType, isNoInfer, isFromNullabilityConstraint)
        }

        if (subType.anyBound(this::isMyTypeVariable)) {
            return simplifyUpperConstraint(subType, superType, isNoInfer) && (answer ?: true)
        } else {
            extractTypeVariableForSubtype(subType, superType, isNoInfer)?.let {
                return simplifyUpperConstraint(it, superType, isNoInfer) && (answer ?: true)
            }

            return simplifyConstraintForPossibleIntersectionSubType(subType, superType, isNoInfer) ?: answer
        }
    }

    // extract type variable only from type like Captured(out T)
    private fun extractTypeVariableForSubtype(subType: CangJieTypeMarker, superType: CangJieTypeMarker, isNoInfer: Boolean): CangJieTypeMarker? =
        with(extensionTypeContext) {

            val typeMarker = subType.asSimpleType()?.asCapturedType() ?: return null

            val projection = typeMarker.typeConstructorProjection()

            // 仓颉语言：没有星投影和 IN/OUT 变型概念，所有泛型都是不变的
            // 简化逻辑：直接获取投影类型
            val projectionType = projection.getType()?.asSimpleType() ?: return null

            if (isMyTypeVariable(projectionType)) {
                simplifyLowerConstraint(projectionType, superType, isNoInfer = isNoInfer)
                if (isMyTypeVariable(superType.asSimpleType() ?: return null)) {
                    addLowerConstraint(superType.typeConstructor(), optionAnyType(), isNoInfer = isNoInfer)
                }
            }

            return if (isMyTypeVariable(projectionType)) {
                projectionType
            } else {
                null
            }
        }

    /**
     * 仓颉语言约束简化规则（简化版）
     *
     * 仓颉语言特性：
     * - Option 是确切的类型，不能改变 Option 标记
     * - T 和 ?T 是两个不同的类型
     * - 类型推断时需要保持 Option 标记不变
     *
     * 约束简化：
     * Foo <: T  => Foo <: T (保持不变)
     * ?Foo <: T => ?Foo <: T (保持不变)
     * Foo <: ?T => Foo <: ?T (保持不变)
     *
     * 注意：与 Kotlin 不同，仓颉不会修改类型的 Option 标记
     */
    private fun simplifyLowerConstraint(
        typeVariable: CangJieTypeMarker,
        subType: CangJieTypeMarker,
        isNoInfer: Boolean,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean = with(extensionTypeContext) {
        // 仓颉语言：直接使用 subType，不做任何 Option 标记的修改
        // Option 是确切的类型，不能被改变
        addLowerConstraint(typeVariable.typeConstructor(), subType, isFromNullabilityConstraint, isNoInfer)
        return true
    }

    /**
     * 简化上界约束
     *
     * 仓颉语言：
     * T <: Foo  => T <: Foo (保持不变)
     * ?T <: Foo => ?T <: Foo (保持不变)
     *
     * 对于 Option 类型变量，检查是否需要添加 Nothing? <: superType 约束
     */
    private fun simplifyUpperConstraint(
        typeVariable: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isNoInfer: Boolean
    ): Boolean = with(extensionTypeContext) {
        val typeVariableLowerBound = typeVariable.lowerBoundIfFlexible()

        // 仓颉语言：简化逻辑，直接使用 superType，不做 Option 标记的修改
        addUpperConstraint(typeVariableLowerBound.typeConstructor(), superType, isNoInfer)

        if (typeVariableLowerBound.isMarkedOption()) {
            // 仓颉语言：检查 superType 是否包含类型变量，或者 ?Nothing <: superType
            return superType.anyBound(::isMyTypeVariable) ||
                    isSubtypeOfByTypeChecker(optionNothingType(), superType)
        }

        return true
    }

    private fun simplifyConstraintForPossibleIntersectionSubType(subType: CangJieTypeMarker, superType: CangJieTypeMarker, isNoInfer: Boolean): Boolean? =
        with(extensionTypeContext) {
            @Suppress("NAME_SHADOWING")
            val subType = subType.lowerBoundIfFlexible()

            if (!subType.typeConstructor().isIntersection()) return null

            assert(!subType.isMarkedOption()) { "Intersection type should not be marked Option!: $subType" }

            // TODO: may be we lose flexibility here
            val subIntersectionTypes = (subType.typeConstructor().supertypes()).map { it.lowerBoundIfFlexible() }

            val typeVariables = subIntersectionTypes.filter(::isMyTypeVariable).takeIf { it.isNotEmpty() } ?: return null
            val notTypeVariables = subIntersectionTypes.filterNot(::isMyTypeVariable)

            // 仓颉语言：简化逻辑，检查非类型变量部分是否为 superType 的子类型
            if (notTypeVariables.isNotEmpty() &&
                AbstractTypeChecker.isSubtypeOf(
                    this as TypeCheckerProviderContext,
                    intersectTypes(notTypeVariables),
                    superType
                )
            ) {
                return true
            }

            // 仓颉语言：使用 AbstractOptionChecker 而非 AbstractNullabilityChecker
            // 考虑 Option 类型的特殊处理
            if (notTypeVariables.any { AbstractOptionChecker.isSubtypeOfAny(this as TypeCheckerProviderContext, it) }) {
                return typeVariables.all { simplifyUpperConstraint(it, superType.withOption(true), isNoInfer) }
            }

            return typeVariables.all { simplifyUpperConstraint(it, superType, isNoInfer) }
        }

    private fun isSubtypeOfByTypeChecker(subType: CangJieTypeMarker, superType: CangJieTypeMarker) =
        AbstractTypeChecker.isSubtypeOf(this as TypeCheckerState, subType, superType)

    private fun assertInputTypes(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Unit = with(typeSystemContext) {
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

    private inline fun CangJieTypeMarker.anyBound(f: (SimpleTypeMarker) -> Boolean) = when (this) {
        is SimpleTypeMarker -> f(this)
        is FlexibleTypeMarker -> with(typeSystemContext) { f(lowerBound()) || f(upperBound()) }
        else -> error("sealed")
    }

}
