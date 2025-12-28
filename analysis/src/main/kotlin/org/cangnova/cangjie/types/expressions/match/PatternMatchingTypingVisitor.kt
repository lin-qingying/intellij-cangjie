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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.VARIABLE
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.getType
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingInternals
import org.cangnova.cangjie.types.expressions.ExpressionTypingVisitor

/**
 * 匹配主体（Subject）
 *
 * 表示 match 表达式中被匹配的对象。支持三种类型：
 * - None: 无主体（用于条件 match）
 * - Type: 类型主体
 * - Expression: 表达式主体
 */
sealed class Subject(
    val element: CjElement?,
    val typeInfo: CangJieTypeInfo?,
    val scopeWithSubject: LexicalScope?,
    var type: CangJieType = typeInfo?.type ?: ErrorUtils.createErrorType(ErrorTypeKind.UNKNOWN_TYPE)
) {

    protected abstract fun createDataFlowValue(
        contextAfterSubject: ExpressionTypingContext,
        builtIns: CangJieBuiltIns
    ): DataFlowValue

    abstract fun makeValueArgument(): ValueArgument?
    abstract val valueExpression: CjExpression?
    open fun getCalleeExpressionForSpecialCall(): CjExpression? = null
    lateinit var dataFlowValue: DataFlowValue; protected set

    fun initDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: CangJieBuiltIns) {
        dataFlowValue = createDataFlowValue(contextAfterSubject, builtIns)
    }

    val dataFlowInfo get() = typeInfo?.dataFlowInfo
    val jumpOutPossible get() = typeInfo?.jumpOutPossible ?: false

    /**
     * 无主体（用于条件 match）
     */
    class None : Subject(null, null, null) {
        override fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: CangJieBuiltIns) =
            DataFlowValue.nullValue(builtIns)

        override fun makeValueArgument(): ValueArgument? = null
        override val valueExpression: CjExpression? get() = null
    }

    /**
     * 类型主体
     */
    class Type(
        typeInfo: CangJieTypeInfo,
        _dataFlowValue: DataFlowValue?,
        context: ExpressionTypingContext
    ) : Subject(null, typeInfo, null) {

        init {
            if (_dataFlowValue != null) {
                dataFlowValue = _dataFlowValue
            }
        }

        override fun createDataFlowValue(
            contextAfterSubject: ExpressionTypingContext,
            builtIns: CangJieBuiltIns
        ): DataFlowValue {
            return if (::dataFlowValue.isInitialized) {
                dataFlowValue
            } else {
                DataFlowValue.nullValue(builtIns)
            }
        }

        override fun makeValueArgument(): ValueArgument? = null
        override val valueExpression: CjExpression? = null
    }

    /**
     * 表达式主体
     */
    class Expression(
        val expression: CjExpression,
        typeInfo: CangJieTypeInfo,
        private val dataFlowValueFactory: DataFlowValueFactory
    ) : Subject(expression, typeInfo, null) {
        override fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: CangJieBuiltIns) =
            dataFlowValueFactory.createDataFlowValue(expression, type, contextAfterSubject)

        override fun makeValueArgument(): ValueArgument =
            org.cangnova.cangjie.resolve.calls.util.CallMaker.makeExternalValueArgument(expression)

        override val valueExpression: CjExpression
            get() = expression
    }
}

/**
 * 模式匹配类型检查访问器
 *
 * 负责处理模式匹配相关的类型检查。所有逻辑委托给 PatternAnalyzer。
 *
 * 支持的语法：
 * - match 表达式
 * - let 表达式
 * - is 表达式
 * - 变量声明中的模式
 * - for-in 循环中的模式
 */
class PatternMatchingTypingVisitor internal constructor(facade: ExpressionTypingInternals) :
    ExpressionTypingVisitor(facade) {

    private val patternAnalyzer = PatternAnalyzer(components, facade)

    /**
     * 访问模式变量声明
     */
    override fun visitPatternVariable(
        variable: CjPatternVariable,
        data: ExpressionTypingContext
    ): CangJieTypeInfo? {
        return patternAnalyzer.analyzePatternVariable(variable, data)
    }

    /**
     * 访问 let 表达式
     */
    override fun visitLetExpression(
        expression: CjLetExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return patternAnalyzer.analyzeLetExpression(expression, context)
    }

    /**
     * 定义 for-in 循环中的模式变量
     */
    fun defineLocalVariablesFromPattern(
        writableScope: LexicalWritableScope,
        casePattern: CjCasePatternElement,
        receiver: ReceiverValue,
        initializer: CjExpression?,
        context: ExpressionTypingContext
    ) {
        patternAnalyzer.defineLocalVariablesFromPattern(
            writableScope, casePattern, receiver, initializer, context
        )
    }

    /**
     * 访问 match 表达式
     */
    override fun visitMatchExpression(
        expression: CjMatchExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return patternAnalyzer.analyzeMatchExpression(expression, context)
    }

    /**
     * 访问 match 表达式（带 isStatement 参数）
     */
    fun visitMatchExpression(
        expression: CjMatchExpression,
        contextWithExpectedType: ExpressionTypingContext,
        @Suppress("UNUSED_PARAMETER") isStatement: Boolean
    ): CangJieTypeInfo {
        return patternAnalyzer.analyzeMatchExpression(expression, contextWithExpectedType)
    }

    /**
     * 访问 is 表达式
     */
    override fun visitIsExpression(
        expression: CjIsExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        return patternAnalyzer.analyzeIsExpression(expression, contextWithExpectedType)
    }
}

/**
 * 扩展函数：检查 match 表达式的穷举性
 */
fun CjMatchExpression.checkExhaustive(context: BindingContext): List<Pattern>? {
    return doCheckExhaustive(this, context)
}

/**
 * 检查类型模式是否覆盖
 */
fun checkTypePattern(condition: CjTypePattern, type: CangJieType?, context: BindingContext): Boolean {
    type ?: return false
    val typeReference = condition.typeReference
    val typeByPsi = typeReference?.getType(context) ?: return false
    return org.cangnova.cangjie.types.checker.CangJieTypeChecker.DEFAULT.equalTypes(typeByPsi, type)
}

/**
 * 检查是否为绑定模式
 */
fun isBindingPattern(pattern: CjBindingPattern, context: BindingContext): Boolean {
    return context[VARIABLE, pattern] != null
}

/**
 * 声明描述符的源元素
 */
val DeclarationDescriptor.sourceElement: SourceElement
    get() = if (this is DeclarationDescriptorWithSource) source else SourceElement.NO_SOURCE
