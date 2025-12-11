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

package org.cangnova.cangjie.types.expressions

import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.diagnostics.reportTypeMismatchDueToScalaLikeNamedFunctionSyntax
import org.cangnova.cangjie.diagnostics.reportTypeMismatchDueToTypeProjection
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import org.cangnova.cangjie.resolve.calls.checkers.NewSchemeOfIntegerOperatorResolutionChecker
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.constants.CompileTimeConstant
import org.cangnova.cangjie.resolve.constants.FloatValueTypeConstant
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import org.cangnova.cangjie.resolve.constants.TypedCompileTimeConstant
import org.cangnova.cangjie.types.isError
import com.intellij.openapi.util.Ref
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.TypeUtils.UNIT_EXPECTED_TYPE
import org.cangnova.cangjie.types.TypeUtils.noExpectedType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo

/**
 * 数据流分析器
 *
 * 负责在表达式类型检查过程中进行数据流分析，包括：
 * - 类型检查和验证
 * - 条件表达式的数据流信息提取
 * - 智能类型转换
 * - 编译期常量类型推导
 *
 * ## 核心功能
 *
 * ### 1. 类型检查
 * [checkType] 系列方法用于检查表达式类型是否符合期望类型：
 * - 子类型判断
 * - 类型不匹配报告
 * - 智能转换可能性检查
 *
 * ### 2. 数据流信息提取
 * [extractDataFlowInfoFromCondition] 从条件表达式中提取类型信息：
 * - `is` 类型检查
 * - `==`/`!=` 相等性检查
 * - `&&`/`||` 逻辑组合
 * - `!` 逻辑取反
 *
 * ### 3. 语句类型检查
 * [checkStatementType] 验证语句上下文中的表达式：
 * - 期望类型必须为 Unit
 * - 报告期望类型不匹配错误
 *
 * ### 4. 编译期常量处理
 * [createCompileTimeConstantTypeInfo] 处理整数和浮点数字面量：
 * - 整数字面量类型推导
 * - 浮点数字面量类型推导
 * - 类型上下文依赖处理
 *
 * ## 使用示例
 *
 * ### 类型检查
 *
 * ```kotlin
 * val typeInfo = analyzer.checkType(
 *     expressionTypeInfo,
 *     expression,
 *     context
 * )
 * ```
 *
 * ### 数据流信息提取
 *
 * ```kotlin
 * val dataFlowInfo = analyzer.extractDataFlowInfoFromCondition(
 *     condition,
 *     whenTrue = true,
 *     context
 * )
 * // 使用 dataFlowInfo 进行智能类型转换
 * ```
 *
 * ### 编译期常量类型推导
 *
 * ```kotlin
 * val typeInfo = analyzer.createCompileTimeConstantTypeInfo(
 *     constantValue,
 *     expression,
 *     context
 * )
 * ```
 *
 * @property additionalTypeCheckers 额外的类型检查器，用于执行自定义检查
 * @property constantExpressionEvaluator 常量表达式求值器，用于编译期计算
 * @property module 模块描述符，表示当前编译的模块
 * @property builtIns 内置类型信息
 * @property facade 表达式类型检查门面
 * @property languageVersionSettings 语言版本设置
 * @property dataFlowValueFactory 数据流值工厂
 * @property cangjieTypeChecker 类型检查器，用于子类型判断
 */
class DataFlowAnalyzer(
    private val additionalTypeCheckers: Iterable<AdditionalTypeChecker>,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val module: ModuleDescriptor,
    private val builtIns: CangJieBuiltIns,
    private val facade: ExpressionTypingFacade,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val cangjieTypeChecker: CangJieTypeChecker
) {

    companion object {
        /**
         * 获取所有可能的类型
         *
         * 结合表达式的原始类型和数据流分析得出的类型信息，
         * 返回表达式可能具有的所有类型。
         *
         * ## 应用场景
         *
         * ### 智能类型转换
         *
         * ```kotlin
         * val x: Any = getValue()
         * if (x is String) {
         *     // 在此分支中，x 的可能类型包括：
         *     // 1. 原始类型：Any
         *     // 2. 数据流类型：String
         *     val allTypes = getAllPossibleTypes(x.type, context, dataFlowValue, languageVersionSettings)
         *     // allTypes = [Any, String]
         * }
         * ```
         *
         * ### 空安全检查
         *
         * ```kotlin
         * val x: String? = getValue()
         * if (x != null) {
         *     // x 的可能类型：[String?, String]
         * }
         * ```
         *
         * @param type 表达式的原始类型
         * @param c 解析上下文，包含数据流信息
         * @param dataFlowValue 数据流值
         * @param languageVersionSettings 语言版本设置
         * @return 所有可能的类型集合
         */
        @JvmStatic
        fun getAllPossibleTypes(
            type: CangJieType,
            c: ResolutionContext<*>,
            dataFlowValue: DataFlowValue,
            languageVersionSettings: LanguageVersionSettings
        ): Collection<CangJieType> {
            val possibleTypes = mutableSetOf(type)
            possibleTypes.addAll(c.dataFlowInfo.getStableTypes(dataFlowValue, languageVersionSettings))
            return possibleTypes
        }
    }

    /**
     * 检查类型信息
     *
     * 对 [CangJieTypeInfo] 中的类型执行检查，返回更新后的类型信息。
     *
     * @param typeInfo 要检查的类型信息
     * @param expression 表达式
     * @param context 解析上下文
     * @return 检查后的类型信息
     */
    fun checkType(
        typeInfo: CangJieTypeInfo,
        expression: CjExpression,
        context: ResolutionContext<*>
    ): CangJieTypeInfo {
        return typeInfo.replaceType(checkType(typeInfo.type, expression, context))
    }

    /**
     * 处理非法语句类型
     *
     * 当表达式在语句上下文中使用不当时调用。
     *
     * ## 处理流程
     *
     * 1. 以独立上下文检查表达式类型
     * 2. 在非调试器上下文中报告 EXPRESSION_EXPECTED 错误
     * 3. 返回空类型信息
     *
     * ## 示例
     *
     * ```kotlin
     * fun example() {
     *     1 + 2  // 表达式作为语句使用，期望返回 Unit
     * }
     * ```
     *
     * @param expression 表达式
     * @param context 表达式类型检查上下文
     * @param facade 表达式类型检查内部接口
     * @return 空类型信息
     */
    fun illegalStatementType(
        expression: CjExpression,
        context: ExpressionTypingContext,
        facade: ExpressionTypingInternals
    ): CangJieTypeInfo {
        facade.checkStatementType(
            expression,
            context.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT)
        )
        if (!context.isDebuggerContext) {
            context.trace.report(EXPRESSION_EXPECTED.on(expression, expression))
        }
        return noTypeInfo(context)
    }

    /**
     * 记录期望类型
     *
     * 将表达式的期望类型记录到 trace 中，用于后续分析和错误报告。
     *
     * ## 期望类型归一化
     *
     * - `UNIT_EXPECTED_TYPE` → `Unit` 类型
     * - `NO_EXPECTED_TYPE` → 不记录
     * - 其他类型 → 直接记录
     *
     * @param trace 绑定跟踪器
     * @param expression 表达式
     * @param expectedType 期望类型
     */
    fun recordExpectedType(trace: BindingTrace, expression: CjExpression, expectedType: CangJieType) {
        if (expectedType != NO_EXPECTED_TYPE) {
            val normalizeExpectedType = if (expectedType == UNIT_EXPECTED_TYPE) {
                builtIns.unitType
            } else {
                expectedType
            }
            trace.record(BindingContext.EXPECTED_EXPRESSION_TYPE, expression, normalizeExpectedType)
        }
    }

    /**
     * 检查类型（带错误报告控制）
     *
     * @param expressionType 表达式类型
     * @param expression 表达式
     * @param context 解析上下文
     * @param reportErrorForTypeMismatch 是否报告类型不匹配错误
     * @return 检查后的类型，可能为 null
     */
    fun checkType(
        expressionType: CangJieType?,
        expression: CjExpression,
        context: ResolutionContext<*>,
        reportErrorForTypeMismatch: Boolean
    ): CangJieType? {
        return checkType(expressionType, expression, context, null, reportErrorForTypeMismatch)
    }

    /**
     * 判断类型是否有 Any 的 equals 方法
     *
     * 检查类型是否使用了 Any 的默认 equals 实现（引用相等性）。
     *
     * ## 判断条件
     *
     * 1. 类型必须是 final 的（不能被继承）
     * 2. 类型没有覆盖 equals 方法
     *
     * ## 用途
     *
     * 在 `==` 比较中：
     * - 如果类型有 Any 的 equals：使用引用相等性
     * - 如果类型覆盖了 equals：使用值相等性
     *
     * ## 示例
     *
     * ```kotlin
     * class Foo  // final, 没有覆盖 equals
     * val f1 = Foo()
     * val f2 = Foo()
     * f1 == f2  // 引用相等性，结果为 false
     *
     * data class Bar(val x: Int)  // 覆盖了 equals
     * val b1 = Bar(1)
     * val b2 = Bar(1)
     * b1 == b2  // 值相等性，结果为 true
     * ```
     *
     * @param type 要检查的类型
     * @param lookupElement 查找元素，用于作用域查找
     * @return 如果类型有 Any 的 equals 则返回 true
     */
    fun typeHasEqualsFromAny(type: CangJieType, lookupElement: CjElement): Boolean {
        val constructor = type.constructor
        // 子类型可能会覆盖非 final 类型的 equals
        if (!constructor.isFinal) return false
        // 检查 equals 是否被覆盖
        return !typeHasOverriddenEquals(type, lookupElement)
    }

    /**
     * 检查类型是否覆盖了 equals 方法
     *
     * TODO: 当前实现返回 false，需要实现实际的覆盖检查逻辑
     *
     * @param type 要检查的类型
     * @param lookupElement 查找元素
     * @return 如果覆盖了 equals 则返回 true
     */
    private fun typeHasOverriddenEquals(type: CangJieType, lookupElement: CjElement): Boolean {
        // TODO: 实现 equals 覆盖检查
        // 当前被注释的实现：
        // - 检查成员函数中的 equals 方法
        // - 验证签名：equals(other: Any): Boolean
        // - 检查是否覆盖了 Any.equals
        return false
    }

    /**
     * 检查类型（完整版本）
     *
     * 执行完整的类型检查流程，包括错误处理和额外检查器。
     *
     * ## 检查流程
     *
     * 1. 去除表达式的括号
     * 2. 记录期望类型
     * 3. 执行内部类型检查
     * 4. 如果没有错误，执行额外的类型检查器
     *
     * @param expressionType 表达式类型，可能为 null
     * @param expressionToCheck 要检查的表达式
     * @param c 解析上下文
     * @param hasError 输出参数，表示是否有错误
     * @param reportErrorForTypeMismatch 是否报告类型不匹配错误
     * @return 检查后的类型，可能为 null
     */
    fun checkType(
        expressionType: CangJieType?,
        expressionToCheck: CjExpression,
        c: ResolutionContext<*>,
        hasError: Ref<Boolean>?,
        reportErrorForTypeMismatch: Boolean
    ): CangJieType? {
        val errorRef = hasError ?: Ref.create(false)
        errorRef.set(false)

        val expression = CjPsiUtil.safeDeparenthesize(expressionToCheck)
        recordExpectedType(c.trace, expression, c.expectedType)

        if (expressionType == null) return null

        val result = checkTypeInternal(expressionType, expression, c, errorRef, reportErrorForTypeMismatch)
        if (errorRef.get() == false) {
            for (checker in additionalTypeCheckers) {
                checker.checkType(expression, expressionType, result, c)
            }
        }

        return result
    }

    /**
     * 从条件表达式中提取数据流信息
     *
     * 分析条件表达式，提取在条件为真或为假时的类型信息。
     *
     * ## 支持的条件类型
     *
     * ### 1. is 表达式
     * ```kotlin
     * if (x is String) {
     *     // x 的类型被细化为 String
     * }
     * ```
     *
     * ### 2. 逻辑操作符 && 和 ||
     * ```kotlin
     * if (x is String && x.length > 0) {
     *     // 组合两个条件的数据流信息
     * }
     * ```
     *
     * ### 3. 相等性检查 == 和 !=
     * ```kotlin
     * if (x == y) {
     *     // x 和 y 被认为相等
     * }
     * if (x != null) {
     *     // x 被细化为非空类型
     * }
     * ```
     *
     * ### 4. 逻辑取反 !
     * ```kotlin
     * if (!(x is String)) {
     *     // 反转条件的数据流信息
     * }
     * ```
     *
     * ## 算法原理
     *
     * ### && 操作符
     * - conditionValue = true: `left.and(right)`
     * - conditionValue = false: `left.or(right)`
     *
     * ### || 操作符
     * - conditionValue = true: `left.or(right)`
     * - conditionValue = false: `left.and(right)`
     *
     * ### == 和 != 操作符
     * - 根据类型是否有 equals from Any 决定使用引用相等性或值相等性
     * - 使用 `equate` 或 `disequate` 更新数据流信息
     *
     * @param condition 条件表达式，可能为 null
     * @param conditionValue 条件的期望值（true 或 false）
     * @param context 表达式类型检查上下文
     * @return 提取的数据流信息
     */
    fun extractDataFlowInfoFromCondition(
        condition: CjExpression?,
        conditionValue: Boolean,
        context: ExpressionTypingContext
    ): DataFlowInfo {
        if (condition == null) return context.dataFlowInfo

        val result = Ref<DataFlowInfo>(null)
        condition.accept(object : CjVisitorUnit() {
            override fun visitIsExpression(expression: CjIsExpression) {
                if (conditionValue) {
                    result.set(context.trace[BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression])
                }
            }

            override fun visitBinaryExpression(expression: CjBinaryExpression) {
                val operationToken = expression.operationToken

                if (OperatorConventions.BOOLEAN_OPERATIONS_NAMES.containsKey(operationToken)) {
                    // 处理 && 和 || 操作符
                    var dataFlowInfo = extractDataFlowInfoFromCondition(expression.left, conditionValue, context)
                    val expressionRight = expression.right

                    if (expressionRight != null) {
                        val and = operationToken == CjTokens.ANDAND
                        val rightInfo = extractDataFlowInfoFromCondition(
                            expressionRight,
                            conditionValue,
                            if (and == conditionValue) context.replaceDataFlowInfo(dataFlowInfo) else context
                        )
                        dataFlowInfo = if (and == conditionValue) {
                            dataFlowInfo.and(rightInfo)
                        } else {
                            dataFlowInfo.or(rightInfo)
                        }
                    }
                    result.set(dataFlowInfo)
                } else {
                    // 处理 == 和 != 操作符
                    val expressionFlowInfo = facade.getTypeInfo(expression, context).dataFlowInfo
                    val left = expression.left ?: return
                    val right = expression.right ?: return

                    val lhsType = context.trace.bindingContext.getType(left) ?: return
                    val rhsType = context.trace.bindingContext.getType(right) ?: return

                    val leftValue = dataFlowValueFactory.createDataFlowValue(left, lhsType, context)
                    val rightValue = dataFlowValueFactory.createDataFlowValue(right, rhsType, context)

                    val equals = when (operationToken) {
                        CjTokens.EQEQ -> true
                        CjTokens.EXCLEQ -> false
                        else -> null
                    }

                    if (equals != null) {
                        result.set(
                            if (equals == conditionValue) {
                                val identityEquals = typeHasEqualsFromAny(lhsType, condition)
                                context.dataFlowInfo
                                    .equate(leftValue, rightValue, identityEquals, languageVersionSettings)
                                    .and(expressionFlowInfo)
                            } else {
                                context.dataFlowInfo
                                    .disequate(leftValue, rightValue, languageVersionSettings)
                                    .and(expressionFlowInfo)
                            }
                        )
                    } else {
                        result.set(expressionFlowInfo)
                    }
                }
            }

            override fun visitUnaryExpression(expression: CjUnaryExpression) {
                val operationTokenType = expression.operationReference.referencedNameElementType
                if (operationTokenType == CjTokens.EXCL) {
                    val baseExpression = expression.baseExpression
                    if (baseExpression != null) {
                        result.set(extractDataFlowInfoFromCondition(baseExpression, !conditionValue, context))
                    }
                } else {
                    visitExpression(expression)
                }
            }

            override fun visitExpression(expression: CjExpression) {
                // 默认从 trace 中获取数据流信息
                result.set(facade.getTypeInfo(expression, context).dataFlowInfo)
            }

            override fun visitParenthesizedExpression(expression: CjParenthesizedExpression) {
                val body = expression.expression
                body?.accept(this)
            }
        })

        // 从效应系统提取额外的数据流信息
        val infoFromEffectSystem = DataFlowInfo.EMPTY
        // TODO: 启用效应系统
        // val infoFromEffectSystem = effectSystem.extractDataFlowInfoFromCondition(
        //     condition, conditionValue, context.trace, DescriptorUtils.getContainingModule(context.scope.ownerDescriptor)
        // )

        return if (result.get() == null) {
            context.dataFlowInfo.and(infoFromEffectSystem)
        } else {
            context.dataFlowInfo.and(result.get()!!)  // .and(infoFromEffectSystem)
        }
    }

    /**
     * 内部类型检查
     *
     * 执行实际的类型检查逻辑。
     *
     * ## 检查规则
     *
     * 1. 无期望类型：直接返回表达式类型
     * 2. 期望类型不可表示：直接返回表达式类型
     * 3. 表达式类型是期望类型的子类型：通过检查
     * 4. 否则：报告类型不匹配错误
     *
     * ## 特殊处理（TODO）
     *
     * - 常量表达式的类型检查
     * - when 表达式的特殊处理
     * - 智能类型转换的可能性检查
     *
     * @param expressionType 表达式类型
     * @param expression 表达式
     * @param c 解析上下文
     * @param hasError 输出参数，表示是否有错误
     * @param reportErrorForTypeMismatch 是否报告类型不匹配错误
     * @return 检查后的类型
     */
    private fun checkTypeInternal(
        expressionType: CangJieType,
        expression: CjExpression,
        c: ResolutionContext<*>,
        hasError: Ref<Boolean>,
        reportErrorForTypeMismatch: Boolean
    ): CangJieType {
        // TODO: 构建器推导类型处理
        // if (!noExpectedType(c.expectedType) && TypeUtilsKt.contains(expressionType, { it is StubTypeForBuilderInference })) {
        //     if (c.inferenceSession is BuilderInferenceSession) {
        //         (c.inferenceSession as BuilderInferenceSession).addExpectedTypeConstraint(expression, expressionType, c.expectedType)
        //     }
        // }

        if (noExpectedType(c.expectedType) || !c.expectedType.constructor.isDenotable ||
            cangjieTypeChecker.isSubtypeOf(expressionType, c.expectedType)
        ) {
            return expressionType
        }

        // TODO: 常量表达式类型检查
        // TODO: when 表达式特殊处理
        // TODO: 智能类型转换检查

        if (reportErrorForTypeMismatch &&
            !c.reportTypeMismatchDueToTypeProjection(expression, c.expectedType, expressionType) &&
            !c.reportTypeMismatchDueToScalaLikeNamedFunctionSyntax(expression, c.expectedType, expressionType)
        ) {
            c.trace.report(TYPE_MISMATCH.on(expression, c.expectedType, expressionType))
        }
        hasError.set(true)
        return expressionType
    }

    /**
     * 检查并确定表达式的类型
     *
     * 此方法主要用于对给定的表达式进行类型检查和验证，判断其是否符合某种特定的类型。
     * 它是类型检查过程中的一个重要方法，帮助确保表达式的正确性和一致性。
     *
     * @param expressionType 表达式类型，被检查的类型，可能为 null，表示尚未确定或不需要特定类型
     * @param expression 需要进行类型检查的表达式对象，不能为空
     * @param context 解析上下文，包含了类型检查时需要的各种环境信息，不能为空
     * @return 返回实际确定的表达式类型，如果无法确定或表达式不符合预期类型，则可能返回 null
     */
    fun checkType(
        expressionType: CangJieType?,
        expression: CjExpression,
        context: ResolutionContext<*>
    ): CangJieType? {
        return checkType(expressionType, expression, context, null, true)
    }

    /**
     * 检查语句类型
     *
     * 验证语句上下文中的表达式是否合法。
     *
     * ## 检查规则
     *
     * 语句的期望类型必须是以下之一：
     * - `NO_EXPECTED_TYPE`
     * - `Unit` 类型
     * - 错误类型（已经报告过错误）
     *
     * 否则报告 EXPECTED_TYPE_MISMATCH 错误。
     *
     * @param expression 表达式
     * @param context 解析上下文
     * @return Unit 类型，或在错误情况下返回 null
     */
    fun checkStatementType(expression: CjExpression, context: ResolutionContext<*>): CangJieType? {
        if (!noExpectedType(context.expectedType) &&
            !CangJieBuiltIns.isUnit(context.expectedType) &&
            !context.expectedType.isError
        ) {
            context.trace.report(EXPECTED_TYPE_MISMATCH.on(expression, context.expectedType))
            return null
        }
        return builtIns.unitType
    }

    /**
     * 创建已检查的类型信息
     *
     * 从类型创建 [CangJieTypeInfo] 并执行类型检查。
     *
     * @param type 类型，可能为 null
     * @param context 解析上下文
     * @param expression 表达式
     * @return 检查后的类型信息
     */
    fun createCheckedTypeInfo(
        type: CangJieType?,
        context: ResolutionContext<*>,
        expression: CjExpression
    ): CangJieTypeInfo {
        return checkType(createTypeInfo(type, context), expression, context)
    }

    /**
     * 创建编译期常量类型信息
     *
     * 根据编译期常量值确定表达式的类型。
     *
     * ## 常量类型推导
     *
     * ### 整数字面量
     * - 独立上下文：根据期望类型推导（Int、Long 等）
     * - 依赖上下文：使用未知整数类型
     *
     * ### 浮点数字面量
     * - 独立上下文：根据期望类型推导（Float、Double 等）
     * - 依赖上下文：使用未知浮点数类型
     *
     * ### 其他常量
     * - 使用常量的类型
     *
     * ## 示例
     *
     * ```kotlin
     * val x = 42          // 独立上下文，推导为 Int
     * val y: Long = 42    // 根据期望类型推导为 Long
     * foo(42)             // 依赖上下文，使用未知整数类型
     * ```
     *
     * @param value 编译期常量值
     * @param expression 表达式
     * @param context 表达式类型检查上下文
     * @return 常量的类型信息
     */
    fun createCompileTimeConstantTypeInfo(
        value: CompileTimeConstant<*>,
        expression: CjExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val expressionType = when (value) {
            is IntegerValueTypeConstant -> {
                if (ContextDependency.INDEPENDENT == context.contextDependency) {
                    val type = value.getType(context.expectedType)
                    constantExpressionEvaluator.updateNumberType(
                        type,
                        expression,
                        context.statementFilter,
                        context.trace
                    )
                    type
                } else {
                    value.unknownIntegerType
                }
            }

            is FloatValueTypeConstant -> {
                if (ContextDependency.INDEPENDENT == context.contextDependency) {
                    val type = value.getType(context.expectedType)
                    constantExpressionEvaluator.updateNumberType(
                        type,
                        expression,
                        context.statementFilter,
                        context.trace
                    )
                    type
                } else {
                    value.unknownIntegerType
                }
            }

            else -> {
                (value as TypedCompileTimeConstant<*>).type
            }
        }

        NewSchemeOfIntegerOperatorResolutionChecker.checkArgument(
            context.expectedType,
            expression,
            context.trace,
            module
        )

        return createCheckedTypeInfo(expressionType, context, expression)
    }
}
