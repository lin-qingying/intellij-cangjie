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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.descriptors.*

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjPsiUtil.deparenthesize
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import org.cangnova.cangjie.resolve.calls.checkers.CallCheckerContext
import org.cangnova.cangjie.resolve.calls.checkers.NewSchemeOfIntegerOperatorResolutionChecker
import org.cangnova.cangjie.resolve.calls.context.CallPosition
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.context.TemporaryTraceAndCache
import org.cangnova.cangjie.resolve.calls.inference.BuilderInferenceSession
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsUtil
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.extensions.AssignResolutionAltererExtension
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.isError
import com.intellij.openapi.util.Ref
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.name.OperatorConventions.getNameForOperationSymbol
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContextUtils
import org.cangnova.cangjie.resolve.binding.TemporaryBindingTrace
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.expressions.match.PatternMatchingTypingVisitor
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo

/**
 * 语句上下文中的表达式类型检查访问者
 *
 * 扩展 [ExpressionTypingVisitor] 以处理语句上下文中的特殊表达式：
 * - 赋值表达式
 * - 赋值操作符（+=、-=、*= 等）
 * - 代码块表达式
 * - 循环表达式
 * - 变量声明
 *
 * ## 核心职责
 *
 * ### 1. 赋值操作
 * [visitAssignment] 处理简单赋值 `a = b`：
 * - 检查左值合法性（可赋值性）
 * - 处理数组访问赋值 `arr[i] = value`
 * - 处理属性 setter 调用
 * - 更新数据流信息
 *
 * ### 2. 赋值操作符
 * [visitAssignmentOperation] 处理复合赋值 `a += b`：
 * - 解析 `plusAssign()` 方法
 * - 回退到 `plus()` + 赋值
 * - 处理操作符歧义
 * - 验证返回类型为 Unit
 *
 * ### 3. 变量声明
 * [visitVariable] 处理变量声明：
 * - 解析变量类型
 * - 添加到作用域
 * - 处理模式匹配声明
 *
 * ### 4. 循环表达式
 * - [visitWhileExpression]: while 循环
 * - [visitDoWhileExpression]: do-while 循环
 * - [visitForExpression]: for 循环
 *
 * ## 赋值操作符解析规则
 *
 * 对于 `a += b`：
 *
 * 1. **检查 plusAssign()**：解析 `a.plusAssign(b)`
 * 2. **检查 plus()**：解析 `a.plus(b)`
 * 3. **歧义处理**：
 *    - 两者都成功：报告 ASSIGN_OPERATOR_AMBIGUITY
 *    - 仅 plusAssign 成功：使用 plusAssign
 *    - 仅 plus 成功：回退到 `a = a + b`
 *
 * ## 使用示例
 *
 * ```kotlin
 * val scope = LexicalWritableScope(...)
 * val visitor = ExpressionTypingVisitorForStatements(
 *     facade,
 *     scope,
 *     basic,
 *     controlStructures,
 *     patterns,
 *     functions
 * )
 *
 * val typeInfo = expression.accept(visitor, context)
 * ```
 *
 * @property scope 可写词法作用域，用于添加新声明
 * @property basic 基本表达式类型访问者
 * @property controlStructures 控制结构类型访问者
 * @property patterns 模式匹配类型访问者
 * @property functions 函数类型访问者
 */
@Suppress("SuspiciousMethodCalls")
class ExpressionTypingVisitorForStatements(
    facade: ExpressionTypingInternals,
    private val scope: LexicalWritableScope,
    private val basic: BasicExpressionTypingVisitor,
    private val controlStructures: ControlStructureTypingVisitor,
    private val patterns: PatternMatchingTypingVisitor,
    private val functions: FunctionsTypingVisitor
) : ExpressionTypingVisitor(facade) {

    companion object {
        /**
         * 从属性 setter 中提炼类型
         *
         * 如果左操作数是属性引用，尝试从 setter 的参数类型中获取期望类型。
         *
         * TODO: 当前实现返回原始类型，需要实现实际的 setter 参数类型提取
         *
         * @param bindingContext 绑定上下文
         * @param leftOperand 左操作数
         * @param leftOperandType 左操作数类型
         * @return 提炼后的类型，或原始类型
         */
        @JvmStatic
        private fun refineTypeFromPropertySetterIfPossible(
            bindingContext: BindingContext,
            leftOperand: CjElement?,
            leftOperandType: CangJieType?
        ): CangJieType? {
            val descriptor = BindingContextUtils.extractVariableFromResolvedCall(bindingContext, leftOperand)

            // TODO: 实现 setter 参数类型提取
            // if (descriptor is PropertyDescriptor) {
            //     val setter = descriptor.setter
            //     if (setter != null) return setter.valueParameters[0].type
            // }

            return leftOperandType
        }

        /**
         * 从属性 inType 中提炼类型
         *
         * 如果左操作数是属性引用，尝试从属性的 inType 中获取类型。
         *
         * ## inType 说明
         *
         * inType 是属性声明中指定的输入类型，用于类型检查时提供更精确的类型信息。
         *
         * @param bindingContext 绑定上下文
         * @param leftOperand 左操作数
         * @param leftOperandType 左操作数类型
         * @return 提炼后的类型，或原始类型
         */
        @JvmStatic
        private fun refineTypeByPropertyInType(
            bindingContext: BindingContext,
            leftOperand: CjElement?,
            leftOperandType: CangJieType?
        ): CangJieType? {
            val descriptor = BindingContextUtils.extractVariableFromResolvedCall(bindingContext, leftOperand)

            if (descriptor is PropertyDescriptor) {
                val inType = descriptor.inType
                if (inType != null) return inType
            }

            return leftOperandType
        }

        /**
         * 检查至少有一个操作
         *
         * 检查解析结果中是否至少有一个具有指定名称的操作。
         *
         * @param calls 解析的调用集合
         * @param operationName 操作名称
         * @return 如果找到至少一个操作则返回 true
         */
        @JvmStatic
        private fun atLeastOneOperation(
            calls: Collection<ResolvedCall<FunctionDescriptor>>,
            operationName: Name
        ): Boolean {
            return calls.any { it.candidateDescriptor.name == operationName }
        }
    }

    /**
     * 访问命名函数
     *
     * 在语句上下文中，命名函数可能是局部函数声明。
     *
     * @param function 命名函数
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitNamedFunction(function: CjNamedFunction, context: ExpressionTypingContext): CangJieTypeInfo {
        return functions.visitNamedFunction(function, context, isDeclaration = function.name != null, scope)
    }

    /**
     * 访问代码块表达式
     *
     * 处理代码块的返回类型，考虑语句上下文。
     *
     * @param expression 代码块表达式
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitBlockExpression(
        expression: CjBlockExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, isStatement = true)
    }

    /**
     * 访问声明
     *
     * 声明在语句上下文中的类型检查。
     *
     * @param dcl 声明
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitDeclaration(dcl: CjDeclaration, context: ExpressionTypingContext): CangJieTypeInfo {
        return createTypeInfo(components.dataFlowAnalyzer.checkStatementType(dcl, context), context)
    }

    /**
     * 访问 if 表达式
     *
     * 在语句上下文中处理 if 表达式。
     *
     * @param expression if 表达式
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitIfExpression(expression: CjIfExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitIfExpression(expression, context)
    }

    /**
     * 访问 Cangjie 元素（默认处理）
     *
     * 对于不支持的元素，报告错误。
     *
     * @param element Cangjie 元素
     * @param context 表达式类型检查上下文
     * @return 空类型信息
     */
    override fun visitCjElement(element: CjElement, context: ExpressionTypingContext): CangJieTypeInfo {
        context.trace.report(UNSUPPORTED.on(element, "in a block"))
        return noTypeInfo(context)
    }

    /**
     * 检查赋值类型
     *
     * 验证赋值表达式的类型是否符合语句上下文的要求。
     *
     * ## 检查规则
     *
     * - 赋值类型必须是 Unit
     * - 如果不是 Unit 且期望类型也不是 Unit，报告错误
     *
     * @param assignmentType 赋值类型
     * @param expression 二元表达式
     * @param context 表达式类型检查上下文
     * @return 检查后的类型（Unit 或 null）
     */
    private fun checkAssignmentType(
        assignmentType: CangJieType?,
        expression: CjBinaryExpression,
        context: ExpressionTypingContext
    ): CangJieType? {
        if (assignmentType != null && !CangJieBuiltIns.isUnit(assignmentType) && !noExpectedType(context.expectedType) &&
            !context.expectedType.isError() && TypeUtils.equalTypes(context.expectedType, assignmentType)
        ) {
            context.trace.report(ASSIGNMENT_TYPE_MISMATCH.on(expression, context.expectedType))
            return null
        }
        return components.dataFlowAnalyzer.checkStatementType(expression, context)
    }

    /**
     * 访问赋值表达式
     *
     * 处理简单赋值 `a = b`。
     *
     * ## 处理流程
     *
     * 1. 检查左操作数类型
     * 2. 检查数组访问赋值的特殊情况
     * 3. 处理赋值扩展（AssignResolutionAltererExtension）
     * 4. 检查右操作数类型
     * 5. 更新数据流信息
     * 6. 检查左值合法性
     * 7. 运行赋值检查器
     *
     * ## 数组访问赋值
     *
     * `arr[i] = value` 被转换为 `arr.set(i, value)`
     *
     * @param expression 赋值表达式
     * @param contextWithExpectedType 包含期望类型的上下文
     * @return 类型信息
     */
    protected fun visitAssignment(
        expression: CjBinaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = contextWithExpectedType
            .replaceExpectedType(NO_EXPECTED_TYPE)
            .replaceScope(scope)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        val leftOperand = expression.left
        val left = deparenthesize(leftOperand)
        val right = expression.right

        // 处理数组访问赋值
        if (left is CjArrayAccessExpression) {
            if (right == null) return noTypeInfo(context)
            val typeInfo = basic.resolveArrayAccessSetMethod(left, right, context, context.trace)
            basic.checkLValue(context.trace, context, left, right, expression, isStatement = true)
            return typeInfo.replaceType(checkAssignmentType(typeInfo.type, expression, contextWithExpectedType))
        }

        val leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(
            left,
            context.replaceCallPosition(CallPosition.VariableAssignment(left, isSet = true)),
            facade
        )

        val bindingContext = context.trace.bindingContext
        val leftType = leftInfo.type

        val expectedType = refineTypeFromPropertySetterIfPossible(bindingContext, leftOperand, leftType)

        // 处理赋值扩展
        val assignAlterers = AssignResolutionAltererExtension.getInstances(expression.project)
        if (assignAlterers.isNotEmpty()) {
            val alteredTypeInfo = assignAlterers
                .firstOrNull { it.needOverloadAssign(expression, leftType, bindingContext) }
                ?.resolveAssign(bindingContext, expression, leftOperand, left, leftInfo, context, components, scope)

            if (alteredTypeInfo != null) {
                return alteredTypeInfo
            }
        }

        var dataFlowInfo = leftInfo.dataFlowInfo
        val resultInfo = if (right != null) {
            val rightInfo = facade.getTypeInfo(
                right,
                context.replaceDataFlowInfo(dataFlowInfo)
                    .replaceExpectedType(expectedType)
                    .replaceCallPosition(CallPosition.VariableAssignment(leftOperand, isSet = false))
            )

            dataFlowInfo = rightInfo.dataFlowInfo
            val rightType = rightInfo.type

            if (left != null && expectedType != null && rightType != null) {
                val leftValue = components.dataFlowValueFactory.createDataFlowValue(left, expectedType, context)
                val rightValue = components.dataFlowValueFactory.createDataFlowValue(right, rightType, context)
                // 我们只能说 rightValue 与 leftValue 有相同的值
                val updatedInfo = rightInfo.replaceDataFlowInfo(dataFlowInfo.assign(leftValue, rightValue))
                NewSchemeOfIntegerOperatorResolutionChecker.checkArgument(
                    expectedType,
                    right,
                    context.trace,
                    components.moduleDescriptor
                )
                updatedInfo
            } else {
                rightInfo
            }
        } else {
            leftInfo
        }

        if (expectedType != null && leftOperand != null) {
            basic.checkLValue(context.trace, context, leftOperand, right, expression, isStatement = false)

            val callCheckerContext = CallCheckerContext(
                context,
                components.deprecationResolver,
                components.moduleDescriptor,
                components.missingSupertypesResolver,
                components.callComponents,
                context.trace
            )
            for (checker in components.assignmentCheckers) {
                checker.check(expression, callCheckerContext)
            }
        }

        checkPropertyInTypeWithWarnings(
            context,
            expression,
            resultInfo.type,
            resultInfo.dataFlowInfo,
            leftOperand,
            leftType,
            expectedType
        )

        return resultInfo.replaceType(
            components.dataFlowAnalyzer.checkStatementType(expression, contextWithExpectedType)
        )
    }

    /**
     * 检查属性 inType 并发出警告
     *
     * 如果属性有 inType，验证赋值的右侧是否符合 inType 的要求。
     *
     * ## 警告条件
     *
     * - 属性有 inType
     * - inType 与 expectedType 不同
     * - 右侧类型不符合 inType
     *
     * @param context 解析上下文
     * @param expression 二元表达式
     * @param rhsType 右侧类型
     * @param rhsDataFlowInfo 右侧数据流信息
     * @param lhsOperand 左侧操作数
     * @param lhsType 左侧类型
     * @param expectedType 期望类型
     */
    private fun checkPropertyInTypeWithWarnings(
        context: ResolutionContext<*>,
        expression: CjBinaryExpression,
        rhsType: CangJieType?,
        rhsDataFlowInfo: DataFlowInfo,
        lhsOperand: CjExpression?,
        lhsType: CangJieType?,
        expectedType: CangJieType?
    ) {
        if (rhsType == null || expectedType == null) return

        val expectedTypeByInType = refineTypeByPropertyInType(context.trace.bindingContext, lhsOperand, lhsType)

        if (expectedTypeByInType != null && expectedType != expectedTypeByInType &&
            !TypeUtils.equalTypes(expectedType, expectedTypeByInType)
        ) {
            val hasErrorsOnTypeChecking = Ref.create(false)
            components.dataFlowAnalyzer.checkType(
                rhsType,
                expression,
                context.replaceExpectedType(expectedTypeByInType)
                    .replaceDataFlowInfo(rhsDataFlowInfo)
                    .replaceCallPosition(CallPosition.VariableAssignment(lhsOperand, isSet = false)),
                hasErrorsOnTypeChecking,
                reportErrorForTypeMismatch = false
            )
            if (hasErrorsOnTypeChecking.get() == true) {
                context.trace.report(TYPE_MISMATCH_WARNING.on(expression, expectedTypeByInType, rhsType))
            }
        }
    }

    /**
     * 访问二元表达式
     *
     * 根据操作符类型分发到相应的处理方法：
     * - `=`: [visitAssignment]
     * - `+=`, `-=` 等: [visitAssignmentOperation]
     * - 其他: 委托给 facade
     *
     * @param expression 二元表达式
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitBinaryExpression(
        expression: CjBinaryExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val operationSign = expression.operationReference
        val operationType = operationSign.referencedNameElementType

        val result = when {
            operationType == CjTokens.EQ -> visitAssignment(expression, context)
            OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType) -> visitAssignmentOperation(
                expression,
                context
            )

            else -> return facade.getTypeInfo(expression, context)
        }

        return components.dataFlowAnalyzer.checkType(result, expression, context)
    }

    /**
     * 访问赋值操作符表达式
     *
     * 处理复合赋值操作符 `+=`, `-=`, `*=` 等。
     *
     * ## 解析策略
     *
     * 对于 `a += b`：
     *
     * 1. **尝试 plusAssign()**：
     *    ```kotlin
     *    a.plusAssign(b)  // 期望返回 Unit
     *    ```
     *
     * 2. **尝试 plus()**：
     *    ```kotlin
     *    a = a.plus(b)  // 如果 a 可赋值
     *    ```
     *
     * 3. **歧义检测**：
     *    - 如果两者都成功：报告 ASSIGN_OPERATOR_AMBIGUITY
     *    - 如果仅一个成功：使用该方法
     *
     * 4. **rem 操作符特殊处理**：
     *    - `%=` 和 `%` 的处理需要特殊逻辑
     *
     * @param expression 二元表达式
     * @param contextWithExpectedType 包含期望类型的上下文
     * @return 类型信息
     */
    protected fun visitAssignmentOperation(
        expression: CjBinaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        // 使用临时跟踪以便在需要时解析数组 set 方法
        val temporary = TemporaryTraceAndCache.create(
            contextWithExpectedType,
            "trace to resolve array set method for binary expression",
            expression
        )
        val context = contextWithExpectedType
            .replaceExpectedType(NO_EXPECTED_TYPE)
            .replaceTraceAndCache(temporary)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        val operationSign = expression.operationReference
        val operationType = operationSign.referencedNameElementType
        val leftOperand = expression.left
        val leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(leftOperand, context, facade)
        val leftType = leftInfo.type

        val right = expression.right
        val left = leftOperand?.let { deparenthesize(it) }

        if (right == null || left == null) {
            temporary.commit()
            return leftInfo.clearType()
        }

        if (leftType == null) {
            val rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(leftInfo.dataFlowInfo))
            context.trace.report(UNRESOLVED_REFERENCE.on(operationSign, operationSign))
            temporary.commit()
            return rightInfo.clearType()
        } else if (!ArgumentTypeResolver.isFunctionLiteralOrCallableReference(right, context) &&
            !context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        ) {
            // 缓存右侧的类型信息，避免重复求值
            facade.getTypeInfo(right, context.replaceContextDependency(ContextDependency.DEPENDENT))
        }

        val receiver = ExpressionReceiver.create(left, leftType, context.trace.bindingContext)

        // 检查 '+=' 和 '+' 操作，并调用定义的操作
        // 检查 '+='
        val name = OperatorConventions.ASSIGNMENT_OPERATIONS[operationType]!!
        val temporaryForAssignmentOperation = TemporaryTraceAndCache.create(
            context,
            "trace to check assignment operation like '+=' for",
            expression
        )
        val assignmentOperationDescriptors = components.callResolver.resolveBinaryCall(
            context.replaceTraceAndCache(temporaryForAssignmentOperation).replaceScope(scope),
            receiver,
            expression,
            name
        )
        val assignmentOperationType = OverloadResolutionResultsUtil.getResultingType(
            assignmentOperationDescriptors,
            context
        )

        val binaryOperationDescriptors: OverloadResolutionResults<FunctionDescriptor>
        val binaryOperationType: CangJieType?
        val temporaryForBinaryOperation = TemporaryTraceAndCache.create(
            context,
            "trace to check binary operation like '+' for",
            expression
        )
        val ignoreReportsTrace = TemporaryBindingTrace.create(context.trace, "Trace for checking assignability")
        var contextForBinaryOperation: ExpressionTypingContext? = null

        val lhsAssignable = basic.checkLValue(ignoreReportsTrace, context, left, right, expression, isStatement = false)

        if (assignmentOperationType == null || lhsAssignable) {
            contextForBinaryOperation = context.replaceTraceAndCache(temporaryForBinaryOperation).replaceScope(scope)

            // 检查 '+'
            // 清除协程推导中右侧的调用信息，因为我们在另一个上下文中第二次分析它
            if (context.inferenceSession is BuilderInferenceSession) {
                context.inferenceSession.clearCallsInfoByContainingElement(right)
            }

            // || 和 && 是不可重载运算符，但是为了处理 ||= 和 &&= 调用 Basic 的 bool 处理
            // TODO: 实现布尔操作符的特殊处理

            val counterpartName = getNameForOperationSymbol(
                OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[operationType]!!
            )
            binaryOperationDescriptors = components.callResolver.resolveBinaryCall(
                contextForBinaryOperation,
                receiver,
                expression,
                counterpartName
            )

            binaryOperationType = OverloadResolutionResultsUtil.getResultingType(binaryOperationDescriptors, context)
        } else {
            binaryOperationDescriptors = OverloadResolutionResultsImpl.nameNotFound()
            binaryOperationType = null
        }

        val type = assignmentOperationType ?: binaryOperationType
        var rightInfo = leftInfo

        val hasRemAssignOperation = atLeastOneOperation(
            assignmentOperationDescriptors.resultingCalls,
            OperatorNameConventions.REM_ASSIGN
        )
        val hasRemBinaryOperation = atLeastOneOperation(
            binaryOperationDescriptors.resultingCalls,
            OperatorNameConventions.REM
        )

        val oneTypeOfModRemOperations = hasRemAssignOperation == hasRemBinaryOperation

        val maybeAmbiguity = assignmentOperationDescriptors.isSuccess &&
                binaryOperationDescriptors.isSuccess &&
                oneTypeOfModRemOperations

        val isResolvedToPlusAssign = assignmentOperationType != null &&
                (assignmentOperationDescriptors.isSuccess || !binaryOperationDescriptors.isSuccess) &&
                (!hasRemBinaryOperation || !binaryOperationDescriptors.isSuccess)

        val rhsResolutionResult: CangJieTypeInfo? = if (maybeAmbiguity || !isResolvedToPlusAssign) {
            completePlusResolution(contextForBinaryOperation, expression, binaryOperationType, left, leftInfo)
        } else {
            null
        }

        when {
            maybeAmbiguity && rhsResolutionResult != null -> {
                // 两者都可用 => 歧义
                val ambiguityResolutionResults = OverloadResolutionResultsUtil.ambiguity(
                    assignmentOperationDescriptors,
                    binaryOperationDescriptors
                )
                context.trace.report(
                    ASSIGN_OPERATOR_AMBIGUITY.on(
                        operationSign,
                        ambiguityResolutionResults.resultingCalls
                    )
                )

                val descriptors = mutableSetOf<DeclarationDescriptor>()
                for (resolvedCall in ambiguityResolutionResults.resultingCalls) {
                    descriptors.add(resolvedCall.resultingDescriptor)
                }
                rightInfo = rhsResolutionResult
                context.trace.record(AMBIGUOUS_REFERENCE_TARGET, operationSign, descriptors)
            }

            isResolvedToPlusAssign -> {
                // 有 'plusAssign()'，执行 a.plusAssign(b)
                temporaryForAssignmentOperation.commit()
                if (!CangJieTypeChecker.DEFAULT.equalTypes(components.builtIns.unitType, assignmentOperationType)) {
                    context.trace.report(
                        ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT.on(
                            operationSign,
                            assignmentOperationDescriptors.resultingDescriptor,
                            operationSign
                        )
                    )
                }
            }

            else -> {
                if (rhsResolutionResult != null) {
                    rightInfo = rhsResolutionResult
                }
                // 只有 'plus()'，尝试 'a = a + b'
                temporaryForBinaryOperation.commit()
                context.trace.record(VARIABLE_REASSIGNMENT, expression)
            }
        }

        temporary.commit()
        return rightInfo.replaceType(checkAssignmentType(type, expression, contextWithExpectedType))
    }

    /**
     * 访问通用表达式
     *
     * 默认委托给 facade 处理。
     *
     * @param expression 表达式
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitExpression(expression: CjExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        return facade.getTypeInfo(expression, context)
    }

    /**
     * 完成 plus 解析
     *
     * 完成二元操作符的解析，检查左值并更新数据流信息。
     *
     * @param context 表达式类型检查上下文，可能为 null
     * @param expression 二元表达式
     * @param binaryOperationType 二元操作类型
     * @param leftDeparentized 去括号后的左操作数
     * @param leftInfo 左操作数类型信息
     * @return 右侧类型信息，失败时返回 null
     */
    private fun completePlusResolution(
        context: ExpressionTypingContext?,
        expression: CjBinaryExpression,
        binaryOperationType: CangJieType?,
        leftDeparentized: CjExpression,
        leftInfo: CangJieTypeInfo
    ): CangJieTypeInfo? {
        val leftOperand = expression.left
        val rightOperand = expression.right

        if (leftOperand == null || rightOperand == null || context == null) return null

        // TODO: 处理数组访问的特殊情况
        // if (leftDeparentized is CjArrayAccessExpression) {
        //     val contextForResolve = context.replaceScope(scope).replaceBindingTrace(...)
        //     basic.resolveImplicitArrayAccessSetMethod(leftDeparentized, rightOperand, contextForResolve, context.trace)
        // }

        val rightInfo = facade.getTypeInfo(rightOperand, context.replaceDataFlowInfo(leftInfo.dataFlowInfo))

        val bindingContext = context.trace.bindingContext
        val leftType = leftInfo.type

        val expectedType = refineTypeFromPropertySetterIfPossible(bindingContext, leftOperand, leftType)

        val hasErrorsOnTypeChecking = Ref.create(false)
        components.dataFlowAnalyzer.checkType(
            binaryOperationType,
            expression,
            context.replaceExpectedType(expectedType)
                .replaceDataFlowInfo(rightInfo.dataFlowInfo)
                .replaceCallPosition(CallPosition.VariableAssignment(leftDeparentized, isSet = false)),
            hasErrorsOnTypeChecking,
            reportErrorForTypeMismatch = true
        )
        basic.checkLValue(context.trace, context, leftOperand, rightOperand, expression, isStatement = false)

        checkPropertyInTypeWithWarnings(
            context,
            expression,
            binaryOperationType,
            rightInfo.dataFlowInfo,
            leftOperand,
            leftType,
            expectedType
        )

        return if (hasErrorsOnTypeChecking.get() != true) rightInfo else null
    }

    /**
     * 访问 while 表达式
     *
     * 在语句上下文中处理 while 循环。
     *
     * @param expression while 表达式
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitWhileExpression(
        expression: CjWhileExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return controlStructures.visitWhileExpression(expression, context, isStatement = true)
    }

    /**
     * 访问 do-while 表达式
     *
     * 在语句上下文中处理 do-while 循环。
     *
     * @param expression do-while 表达式
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitDoWhileExpression(
        expression: CjDoWhileExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return controlStructures.visitDoWhileExpression(expression, context, isStatement = true)
    }

    /**
     * 访问 for 表达式
     *
     * 在语句上下文中处理 for 循环。
     *
     * @param expression for 表达式
     * @param context 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitForExpression(expression: CjForExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitForExpression(expression, context, isStatement = true)
    }

    /**
     * 访问变量声明
     *
     * 处理变量声明并添加到作用域。
     *
     * ## 处理流程
     *
     * ### 简单变量声明
     * ```kotlin
     * let x = 42
     * ```
     * 1. 解析变量类型
     * 2. 创建变量描述符
     * 3. 添加到作用域
     *
     * ### 模式匹配声明
     * ```kotlin
     * let (a, b) = pair
     * ```
     * 委托给 patterns 访问者处理
     *
     * @param variable 变量声明
     * @param data 表达式类型检查上下文
     * @return 类型信息
     */
    override fun visitVariable(variable: CjVariable, data: ExpressionTypingContext): CangJieTypeInfo {
        return if (variable.pattern == null) {
            // 简单变量声明
            val (typeInfo, variableDescriptor) = components.localVariableResolver.process(
                variable,
                data,
                scope,
                facade
            )
            scope.addVariableDescriptor(variableDescriptor)
            typeInfo
        } else {
            // 模式匹配声明
            patterns.visitVariable(variable, data.replaceScope(scope))
        }
    }

    // TODO: 解构声明支持
    // /**
    //  * 解构声明
    //  *
    //  * @param multiDeclaration 解构声明
    //  * @param context 表达式类型检查上下文
    //  * @return 类型信息
    //  */
    // override fun visitDestructuringDeclaration(
    //     multiDeclaration: CjDestructuringDeclaration,
    //     context: ExpressionTypingContext
    // ): CangJieTypeInfo {
    //     val initializer = multiDeclaration.initializer
    //     if (initializer == null) {
    //         context.trace.report(INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION.on(multiDeclaration))
    //     }
    //
    //     val expressionReceiver = initializer?.let {
    //         ExpressionTypingUtils.getExpressionReceiver(
    //             facade,
    //             it,
    //             context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(ContextDependency.INDEPENDENT)
    //         )
    //     }
    //
    //     components.destructuringDeclarationResolver.defineLocalVariablesFromDestructuringDeclaration(
    //         scope,
    //         multiDeclaration,
    //         expressionReceiver,
    //         initializer,
    //         context
    //     )
    //     components.modifiersChecker.withTrace(context.trace).checkModifiersForDestructuringDeclaration(multiDeclaration)
    //     components.identifierChecker.checkDeclaration(multiDeclaration, context.trace)
    //
    //     return if (expressionReceiver == null) {
    //         noTypeInfo(context)
    //     } else {
    //         facade.getTypeInfo(initializer, context)
    //             .replaceType(components.dataFlowAnalyzer.checkStatementType(multiDeclaration, context))
    //     }
    // }
}
