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
 */

package org.cangnova.cangjie.types.expressions.match

import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.NOT_ENUM_MATCH
import org.cangnova.cangjie.diagnostics.infos.errors.TUPLE_ARGS_MISMATCH
import org.cangnova.cangjie.diagnostics.infos.errors.TUPLE_ARGS_TOO_FEW
import org.cangnova.cangjie.diagnostics.infos.errors.TUPLE_PATTERN_TYPE_MISMATCH
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.referenceExpression
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.COMPILE_TIME_VALUE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.PATTERN
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.deccriptorClass
import org.cangnova.cangjie.types.expressions.ExpressionTypingComponents
import org.cangnova.cangjie.types.expressions.ExpressionTypingInternals
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.isBuiltinTupleType
import org.cangnova.cangjie.types.isEnum

/**
 * 模式解析配置
 *
 * 控制模式解析的行为，如是否尝试解析枚举、变量可变性等。
 *
 * @property bindEnumType 是否绑定枚举类型信息
 * @property isVar 声明的变量是否可变
 * @property isLocal 是否为局部变量
 * @property bindEnumEntry 是否尝试将绑定模式解析为枚举变体
 * @property visibility 变量可见性（仅用于非局部变量）
 */
data class PatternResolverConfig(
    val bindEnumType: Boolean = true,
    val isVar: Boolean = false,
    val isLocal: Boolean = true,
    val bindEnumEntry: Boolean = true,
    val visibility: org.cangnova.cangjie.descriptors.DescriptorVisibility? = null
)

/**
 * 模式解析器（PatternResolver）
 *
 * 负责将 PSI 模式元素转换为语义 Pattern 对象。
 * 这是模式分析的核心组件，处理所有模式类型的解析和类型检查。
 *
 * ## 核心职责
 *
 * 1. **模式解析**: 将 CjCasePatternElement 转换为 Pattern
 * 2. **类型推导**: 确定模式匹配的类型
 * 3. **枚举识别**: 区分绑定模式和枚举模式
 * 4. **错误报告**: 报告模式相关的语义错误
 *
 * ## 模式类型处理
 *
 * | PSI 类型 | 转换结果 |
 * |---------|---------|
 * | CjWildcardPattern | PatternKind.Wild |
 * | CjBindingPattern | PatternKind.Binding 或 PatternKind.Enum |
 * | CjConstantPattern | PatternKind.Const |
 * | CjTypePattern | PatternKind.Type |
 * | CjTuplePattern | PatternKind.Tuple |
 * | CjEnumPattern | PatternKind.Enum |
 *
 * ## 绑定模式 vs 枚举模式
 *
 * 当遇到 `case Foo =>` 时，需要判断 `Foo` 是：
 * - 变量绑定：匹配任何值并绑定到 `Foo`
 * - 枚举变体：匹配特定的枚举构造器
 *
 * 解析策略：
 * 1. 首先尝试在枚举上下文中解析
 * 2. 如果解析成功且类型匹配，作为枚举模式
 * 3. 否则作为绑定模式
 *
 * @see Pattern
 * @see PatternKind
 * @see BindingCollector
 */
class PatternResolver(
    private val components: ExpressionTypingComponents,
    private val facade: ExpressionTypingInternals
) {

    /**
     * 解析模式元素
     *
     * @param element PSI 模式元素
     * @param context 模式分析上下文
     * @param config 解析配置
     * @return 解析得到的 Pattern 对象
     */
    fun resolve(
        element: CjCasePatternElement,
        context: ExtendedPatternContext,
        config: PatternResolverConfig = PatternResolverConfig()
    ): Pattern {
        val pattern = when (element) {
            is CjWildcardPattern -> resolveWildcard(element, context)
            is CjBindingPattern -> resolveBinding(element, context, config)
            is CjConstantPattern -> resolveConstant(element, context)
            is CjTypePattern -> resolveType(element, context, config)
            is CjTuplePattern -> resolveTuple(element, context, config)
            is CjEnumPattern -> resolveEnum(element, context, config)
            else -> Pattern.Error
        }

        // 记录解析结果
        context.typingContext.trace.record(PATTERN, element, pattern)
        return pattern
    }

    /**
     * 解析通配符模式
     */
    private fun resolveWildcard(
        element: CjWildcardPattern,
        context: ExtendedPatternContext
    ): Pattern {
        return Pattern(context.subjectType, PatternKind.Wild, element)
    }

    /**
     * 解析绑定模式
     *
     * 首先尝试解析为枚举变体，失败后作为普通变量绑定处理。
     */
    private fun resolveBinding(
        element: CjBindingPattern,
        context: ExtendedPatternContext,
        config: PatternResolverConfig
    ): Pattern {
        val expression = element.expression

        // 如果配置允许，尝试解析为枚举变体
        if (config.bindEnumEntry && expression != null) {
            val enumResult = tryResolveAsEnumVariant(element, expression, context)
            if (enumResult != null) {
                return enumResult
            }
        }

        // 作为绑定模式处理
        return Pattern(
            context.subjectType,
            PatternKind.Binding(context.subjectType, element.text),
            element
        )
    }

    /**
     * 尝试将绑定模式解析为枚举变体
     *
     * @return 如果成功解析为枚举变体则返回 Pattern，否则返回 null
     */
    private fun tryResolveAsEnumVariant(
        element: CjBindingPattern,
        expression: CjExpression,
        context: ExtendedPatternContext
    ): Pattern? {
        val typeInfo = if (context.subjectType.isEnum) {
            facade.getTypeInfoByCaseEnum(
                expression,
                emptyList(),
                context.typingContext.replaceExpectedType(context.subjectType),
                false
            )
        } else {
            facade.getTypeInfoByCaseEnum(expression, emptyList(), context.typingContext, false)
        }

        if (typeInfo.type == null) {
            return null
        }

        // 类型检查
        if (!CangJieTypeChecker.DEFAULT.equalTypes(typeInfo.type, context.subjectType)) {
            context.typingContext.trace.report(NOT_ENUM_MATCH.on(expression))
        }

        val enumSource = typeInfo.type.deccriptorClass?.source?.getPsi() as? CjEnum
            ?: return Pattern(typeInfo.type, PatternKind.Error, element)

        val enumEntry = context.typingContext.trace[REFERENCE_TARGET, expression.referenceExpression()!!]
        val enumEntrySource = enumEntry?.sourceElement?.getPsi() as? CjEnumConstructor
            ?: return Pattern(typeInfo.type, PatternKind.Error, element)

        return Pattern(
            typeInfo.type,
            PatternKind.Enum(enumSource, enumEntrySource, emptyList()),
            element
        )
    }

    /**
     * 解析常量模式
     */
    private fun resolveConstant(
        element: CjConstantPattern,
        context: ExtendedPatternContext
    ): Pattern {
        val expression = element.expression ?: return Pattern.Error
        val typeInfo = facade.getTypeInfo(expression, context.typingContext)
        val type = typeInfo.type ?: return Pattern.Error

        // 类型兼容性检查
        checkTypeCompatibility(context, type, context.subjectType, expression)

        val constantValue = context.typingContext.trace[COMPILE_TIME_VALUE, expression]
            ?.toConstantValue(type)
            ?: return Pattern.Error

        return Pattern(context.subjectType, PatternKind.Const(constantValue), element)
    }

    /**
     * 解析类型模式
     */
    private fun resolveType(
        element: CjTypePattern,
        context: ExtendedPatternContext,
        config: PatternResolverConfig
    ): Pattern {
        val typeRef = element.typeReference
        val type = typeRef?.let {
            components.typeResolver.resolveType(
                context.typingContext.scope,
                it,
                context.typingContext.trace,
                false
            )
        } ?: org.cangnova.cangjie.types.ErrorUtils.errorVariableType

        return Pattern(context.subjectType, PatternKind.Type(type, element.text), element)
    }

    /**
     * 解析元组模式
     */
    private fun resolveTuple(
        element: CjTuplePattern,
        context: ExtendedPatternContext,
        config: PatternResolverConfig
    ): Pattern {
        val patterns = element.patterns
        val patternSize = patterns.size

        // 检查主体类型是否为元组
        if (!context.subjectType.isBuiltinTupleType) {
            context.typingContext.trace.report(TUPLE_PATTERN_TYPE_MISMATCH.on(element, context.subjectType))
            return Pattern.Error
        }

        // 检查元组大小
        if (patternSize < 1) {
            context.typingContext.trace.report(TUPLE_ARGS_TOO_FEW.on(element))
            return Pattern.Error
        }

        val expectedSize = context.subjectType.arguments.size
        if (patternSize != expectedSize) {
            context.typingContext.trace.report(TUPLE_ARGS_MISMATCH.on(element, expectedSize, patternSize))
            return Pattern.Error
        }

        // 递归解析子模式
        val subPatterns = patterns.mapIndexedNotNull { index, subElement ->
            val expectedType = context.subjectType.arguments[index].type
            val subSubject = Subject.Expression(
                subElement,
                createTypeInfo(expectedType),
                components.dataFlowValueFactory
            )
            val subContext = ExtendedPatternContext(
                subject = subSubject,
                typingContext = context.typingContext,
                source = context.source,
                expectedType = expectedType
            )
            resolve(subElement, subContext, config)
        }

        return Pattern(context.subjectType, PatternKind.Tuple(subPatterns), element)
    }

    /**
     * 解析枚举模式
     */
    private fun resolveEnum(
        element: CjEnumPattern,
        context: ExtendedPatternContext,
        config: PatternResolverConfig
    ): Pattern {
        val expression = element.expression

        val typeInfo = if (context.subjectType.isEnum) {
            expression?.let {
                facade.getTypeInfoByCaseEnum(
                    it,
                    element.patterns,
                    context.typingContext.replaceExpectedType(context.subjectType)
                )
            }
        } else {
            expression?.let {
                facade.getTypeInfoByCaseEnum(it, element.patterns, context.typingContext)
            }
        }

        if (typeInfo?.type == null) {
            expression?.let { context.typingContext.trace.report(NOT_ENUM_MATCH.on(it)) }
            return Pattern.Error
        }

        if (!CangJieTypeChecker.DEFAULT.equalTypes(typeInfo.type, context.subjectType)) {
            expression?.let { context.typingContext.trace.report(NOT_ENUM_MATCH.on(it)) }
        }

        val enumSource = typeInfo.type.deccriptorClass?.source?.getPsi() as? CjEnum
            ?: return Pattern(typeInfo.type, PatternKind.Error, element)

        val enumEntry = context.typingContext.trace[REFERENCE_TARGET, expression?.referenceExpression()!!]
            as? EnumConstructorDescriptor
            ?: return Pattern(typeInfo.type, PatternKind.Error, element)
        val enumEntrySource = enumEntry.sourceElement.getPsi() as? CjEnumConstructor
            ?: return Pattern(typeInfo.type, PatternKind.Error, element)

        // 解析子模式
        val valueParameters = enumEntry.valueParameters
        val subPatterns = try {
            valueParameters.mapIndexedNotNull { index, param ->
                val expectedType = param.type
                val subElement = element.patterns.getOrNull(index) ?: return@mapIndexedNotNull null
                val subSubject = Subject.Expression(
                    subElement,
                    createTypeInfo(expectedType),
                    components.dataFlowValueFactory
                )
                val subContext = ExtendedPatternContext(
                    subject = subSubject,
                    typingContext = context.typingContext,
                    source = context.source,
                    expectedType = expectedType
                )
                resolve(subElement, subContext, config)
            }
        } catch (e: IndexOutOfBoundsException) {
            return Pattern.Error
        }

        return Pattern(typeInfo.type, PatternKind.Enum(enumSource, enumEntrySource, subPatterns), element)
    }

    /**
     * 检查类型兼容性
     */
    private fun checkTypeCompatibility(
        context: ExtendedPatternContext,
        patternType: CangJieType,
        subjectType: CangJieType,
        reportOn: CjElement
    ) {
        if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(patternType, subjectType) &&
            !CangJieTypeChecker.DEFAULT.isSubtypeOf(subjectType, patternType)) {
            // 类型不兼容，可以报告错误
            // context.typingContext.trace.report(INCOMPATIBLE_TYPES.on(reportOn, patternType, subjectType))
        }
    }

    companion object {
        /**
         * 检查模式元素是否为通配符模式
         */
        fun isWildcard(element: CjCasePatternElement): Boolean {
            return element is CjWildcardPattern
        }

        /**
         * 检查模式元素是否为绑定模式
         */
        fun isBinding(element: CjCasePatternElement): Boolean {
            return element is CjBindingPattern
        }
    }
}
