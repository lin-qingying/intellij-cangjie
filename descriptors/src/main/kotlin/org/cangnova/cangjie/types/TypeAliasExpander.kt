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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.error.ErrorTypeKind

/**
 * 类型别名展开器
 *
 * 负责将类型别名（type alias）展开为其底层类型的完整实现。
 * 类型别名是仓颉语言中的一个重要特性，允许为现有类型创建新的名称。
 *
 * 主要功能：
 * - 递归展开嵌套的类型别名
 * - 处理类型参数替换
 * - 检测并报告循环依赖
 * - 保持类型注解和可选性信息
 * - 处理类型投影的方差计算
 *
 * 示例：
 * ```kotlin
 * typealias StringMap<T> = Map<String, T>
 * typealias UserMap = StringMap<User>
 * // UserMap 会被展开为 Map<String, User>
 * ```
 *
 * @param reportStrategy 用于报告类型别名展开过程中的错误和冲突的策略
 * @param shouldCheckBounds 是否检查类型参数边界约束
 */
class TypeAliasExpander(
    private val reportStrategy: TypeAliasExpansionReportStrategy,
    private val shouldCheckBounds: Boolean
){

    /**
     * 不保留缩写形式的类型别名展开
     *
     * 将类型别名完全展开为其底层类型，不保留原始别名信息作为缩写。
     * 这种展开方式完全"忘记"原始的类型别名，只返回最终的底层类型。
     *
     * @param typeAliasExpansion 要展开的类型别名信息
     * @param attributes 类型属性（注解、修饰符等）
     * @return 完全展开的底层类型
     */
    fun expandWithoutAbbreviation(typeAliasExpansion: TypeAliasExpansion, attributes: TypeAttributes) =
        expandRecursively(
            typeAliasExpansion, attributes,
            isOption = false, recursionDepth = 0, withAbbreviatedType = false
        )

    /**
     * 展开非参数类型投影
     *
     * 处理不是类型参数替换的类型投影的展开。这包括：
     * - 动态类型（直接返回）
     * - 类型参数（保持原样）
     * - 类型别名（递归展开）
     * - 普通类型（替换参数）
     *
     * @param originalProjection 原始的类型投影
     * @param typeAliasExpansion 当前的类型别名展开上下文
     * @param recursionDepth 递归深度，用于检测无限递归
     * @return 展开后的类型投影
     */
    private fun expandNonArgumentTypeProjection(
        originalProjection: TypeProjection,
        typeAliasExpansion: TypeAliasExpansion,
        recursionDepth: Int
    ): TypeProjection {
        // 解包原始类型，移除包装层
        val originalType = originalProjection.type.unwrap()

        // 动态类型不需要展开，直接返回
        if (originalType.isDynamic()) return originalProjection

        val type = originalType.asSimpleType()

        // TODO: 添加错误类型和无需展开类型的检查
        //        if (type.isError || !type.requiresTypeAliasExpansion()) {
        //            return originalProjection
        //        }

        val typeConstructor = type.constructor
        val typeDescriptor = typeConstructor.declarationDescriptor

        // 确保类型构造器的参数数量与类型参数数量一致
        assert(typeConstructor.parameters.size == type.arguments.size) { "Unexpected malformed type: $type" }

        return when (typeDescriptor) {
            // 类型参数描述符：直接返回原投影，无需展开
            is TypeParameterDescriptor -> {
                originalProjection
            }
            // 类型别名描述符：需要递归展开
            is TypeAliasDescriptor -> {
                // 检测循环递归
                if (typeAliasExpansion.isRecursion(typeDescriptor)) {
                    reportStrategy.recursiveTypeAlias(typeDescriptor)
                    return TypeProjectionImpl(
                        Variance.INVARIANT,
                        ErrorUtils.createErrorType(
                            ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeDescriptor.name.toString())
                    )
                }

                // 递归展开所有类型参数
                val expandedArguments = type.arguments.mapIndexed { i, typeAliasArgument ->
                    expandTypeProjection(typeAliasArgument, typeAliasExpansion, typeConstructor.parameters[i], recursionDepth + 1)
                }

                // 创建嵌套的类型别名展开上下文
                val nestedExpansion =
                    TypeAliasExpansion.create(typeAliasExpansion, typeDescriptor, expandedArguments)

                // 递归展开嵌套的类型别名
                val nestedExpandedType = expandRecursively(
                    nestedExpansion, type.attributes,
                    isOption = type.isOption,
                    recursionDepth = recursionDepth + 1,
                    withAbbreviatedType = false
                )

                // 替换当前类型的参数
                val substitutedType = type.substituteArguments(typeAliasExpansion, recursionDepth)

                // 动态类型不能被缩写 - 会单独报告
                val typeWithAbbreviation =
                    if (nestedExpandedType.isDynamic()) nestedExpandedType else nestedExpandedType.withAbbreviation(substitutedType)

                TypeProjectionImpl(originalProjection.projectionKind, typeWithAbbreviation)
            }
            // 其他类型描述符：只需要替换参数
            else -> {
                val substitutedType = type.substituteArguments(typeAliasExpansion, recursionDepth)

                // TODO: 添加类型参数替换检查
                //                checkTypeArgumentsSubstitution(type, substitutedType)

                TypeProjectionImpl(originalProjection.projectionKind, substitutedType)
            }
        }
    }
    /**
     * 替换简单类型的类型参数
     *
     * 对给定的简单类型进行类型参数替换，将其中的类型别名参数展开为实际类型。
     * 此方法保持原始类型的可选性信息。
     *
     * @param typeAliasExpansion 类型别名展开上下文
     * @param recursionDepth 当前递归深度
     * @return 参数替换后的新简单类型
     */
    private fun SimpleType.substituteArguments(typeAliasExpansion: TypeAliasExpansion, recursionDepth: Int): SimpleType {
        val typeConstructor = this.constructor

        // 逐个展开并替换类型参数
        val substitutedArguments = this.arguments.mapIndexed { i, originalArgument ->
            val projection = expandTypeProjection(
                originalArgument, typeAliasExpansion, typeConstructor.parameters[i], recursionDepth + 1
            )

            // 保持原始参数的可选性信息
            TypeProjectionImpl(
                projection.projectionKind,
                TypeUtils.makeOptionalIfNeeded(projection.type, originalArgument.type.isOption)
            )
        }

        return this.replace(newArguments = substitutedArguments)
    }
    /**
     * 展开类型投影
     *
     * 这是类型别名展开的核心方法，负责展开单个类型投影。
     * 处理类型参数替换、方差计算和属性合并。
     *
     * @param underlyingProjection 底层的类型投影
     * @param typeAliasExpansion 类型别名展开上下文
     * @param typeParameterDescriptor 对应的类型参数描述符（如果有）
     * @param recursionDepth 当前递归深度
     * @return 展开后的类型投影
     */
    private fun expandTypeProjection(
        underlyingProjection: TypeProjection,
        typeAliasExpansion: TypeAliasExpansion,
        typeParameterDescriptor: TypeParameterDescriptor?,
        recursionDepth: Int
    ): TypeProjection {
        // TODO: 重构 TypeSubstitutor 以引入自定义诊断
//        assertRecursionDepth(recursionDepth, typeAliasExpansion.descriptor)

        // TODO: 处理星号投影
//        if (underlyingProjection.isStarProjection) return TypeUtils.makeStarProjection(typeParameterDescriptor!!)

        val underlyingType = underlyingProjection.type
        // 尝试获取类型参数的替换
        val argument = typeAliasExpansion.getReplacement(underlyingType.constructor)
            ?: return expandNonArgumentTypeProjection(
                underlyingProjection,
                typeAliasExpansion,
                recursionDepth
            )

        // TODO: 处理星号投影参数
//        if (argument.isStarProjection) return TypeUtils.makeStarProjection(typeParameterDescriptor!!)

        val argumentType = argument.type.unwrap()

        // 计算最终的方差
        val resultingVariance = run {
            val argumentVariance = argument.projectionKind
            val underlyingVariance = underlyingProjection.projectionKind

            // 计算替换的方差
            val substitutionVariance =
                when {
                    underlyingVariance == argumentVariance -> argumentVariance
                    underlyingVariance == Variance.INVARIANT -> argumentVariance
                    argumentVariance == Variance.INVARIANT -> underlyingVariance
                    else -> {
                        // 方差冲突，报告错误
                        reportStrategy.conflictingProjection(typeAliasExpansion.descriptor, typeParameterDescriptor, argumentType)
                        argumentVariance
                    }
                }

            val parameterVariance = typeParameterDescriptor?.variance ?: Variance.INVARIANT

            // 结合参数方差和替换方差
            when {
                parameterVariance == substitutionVariance -> substitutionVariance
                parameterVariance == Variance.INVARIANT -> substitutionVariance
                substitutionVariance == Variance.INVARIANT -> Variance.INVARIANT
                else -> {
                    // 参数方差冲突，报告错误
                    reportStrategy.conflictingProjection(typeAliasExpansion.descriptor, typeParameterDescriptor, argumentType)
                    substitutionVariance
                }
            }
        }

        // TODO: 检查重复的注解
//        checkRepeatedAnnotations(underlyingType.annotations, argumentType.annotations)

        // 合并类型属性和可选性信息
        val substitutedType =
            if (argumentType is DynamicType)
                argumentType.combineAttributes(underlyingType.attributes)
            else
                argumentType.asSimpleType().combineOptionAndAnnotations(underlyingType)

        return TypeProjectionImpl(resultingVariance, substitutedType)
    }
    /**
     * 合并简单类型的可选性信息
     *
     * 根据源类型的可选性信息，为当前简单类型添加或保持可选性。
     *
     * @param fromType 源类型，用于获取可选性信息
     * @return 合并可选性后的简单类型
     */
    private fun SimpleType.combineOption(fromType: CangJieType) =
        TypeUtils.makeOptionalIfNeeded(this, fromType.isOption)

    /**
     * 合并简单类型的可选性和注解信息
     *
     * 同时合并可选性和类型属性（注解、修饰符等）。
     *
     * @param fromType 源类型，提供可选性和属性信息
     * @return 合并后的简单类型
     */
    private fun SimpleType.combineOptionAndAnnotations(fromType: CangJieType) =
        combineOption(fromType).combineAttributes(fromType.attributes)

    /**
     * 创建合并的类型属性
     *
     * 将新的类型属性与当前类型的属性合并。错误类型保持原有属性。
     *
     * @param newAttributes 要合并的新属性
     * @return 合并后的类型属性
     */
    private fun CangJieType.createdCombinedAttributes(newAttributes: TypeAttributes): TypeAttributes {
        if (isError) return attributes

        return newAttributes.add(attributes)
    }

    /**
     * 为动态类型合并属性
     *
     * @param newAttributes 要合并的新属性
     * @return 属性合并后的动态类型
     */
    private fun DynamicType.combineAttributes(newAttributes: TypeAttributes): DynamicType =
        replaceAttributes(createdCombinedAttributes(newAttributes))

    /**
     * 为简单类型合并属性
     *
     * @param newAttributes 要合并的新属性
     * @return 属性合并后的简单类型
     */
    private fun SimpleType.combineAttributes(newAttributes: TypeAttributes): SimpleType =
        if (isError) this else replace(newAttributes = createdCombinedAttributes(newAttributes))

    /**
     * 递归展开类型别名
     *
     * 这是类型别名展开的主要递归方法，完全展开类型别名到其底层类型。
     * 处理嵌套的类型别名、属性合并和可选的缩写保留。
     *
     * @param typeAliasExpansion 要展开的类型别名信息
     * @param attributes 额外的类型属性
     * @param isOption 是否为可选类型
     * @param recursionDepth 当前递归深度
     * @param withAbbreviatedType 是否保留缩写类型信息
     * @return 完全展开的简单类型
     */
    private fun expandRecursively(
        typeAliasExpansion: TypeAliasExpansion,
        attributes: TypeAttributes,
        isOption: Boolean,
        recursionDepth: Int,
        withAbbreviatedType: Boolean
    ): SimpleType {
        // 创建底层类型的不变投影
        val underlyingProjection = TypeProjectionImpl(
            Variance.INVARIANT,
            typeAliasExpansion.descriptor.underlyingType
        )
        
        // 展开底层类型投影
        val expandedProjection = expandTypeProjection(underlyingProjection, typeAliasExpansion, null, recursionDepth)
        val expandedType = expandedProjection.type.asSimpleType()

        // 如果展开结果是错误类型，直接返回
        if (expandedType.isError) return expandedType

        // 确保结果投影是不变的
        assert(expandedProjection.projectionKind == Variance.INVARIANT) {
            "Type alias expansion: result for ${typeAliasExpansion.descriptor} is ${expandedProjection.projectionKind}, should be invariant"
        }

        // TODO: 检查重复的注解
//        checkRepeatedAnnotations(expandedType.annotations, attributes.annotations)
        
        // 合并额外的属性并应用可选性
        val expandedTypeWithExtraAnnotations =
            expandedType.combineAttributes(attributes).let { TypeUtils.makeOptionalIfNeeded(it, isOption) }

        // 根据需要添加缩写信息
        return if (withAbbreviatedType)
            expandedTypeWithExtraAnnotations.withAbbreviation(typeAliasExpansion.createAbbreviation(attributes, isOption))
        else
            expandedTypeWithExtraAnnotations
    }

    private fun TypeAliasExpansion.createAbbreviation(attributes: TypeAttributes, isOption: Boolean) =
       CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
            attributes,
            descriptor.typeConstructor,
            arguments,
            isOption,
            MemberScope.Empty
        )

    fun expand(typeAliasExpansion: TypeAliasExpansion, attributes: TypeAttributes) =
        expandRecursively(
            typeAliasExpansion, attributes,
            isOption = false, recursionDepth = 0, withAbbreviatedType = true
        )

    companion object{
        val NON_REPORTING =
            TypeAliasExpander(TypeAliasExpansionReportStrategy.DO_NOTHING, false)
    }
}

