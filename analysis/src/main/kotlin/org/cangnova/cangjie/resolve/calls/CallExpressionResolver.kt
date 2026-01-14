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

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.AstLoadingFilter
import jakarta.inject.Inject
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.UNNECESSARY_SAFE_CALL
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.IS_FUNC
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.recordDataFlowInfo
import org.cangnova.cangjie.resolve.binding.recordScope
import org.cangnova.cangjie.resolve.calls.context.*
import org.cangnova.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsUtil
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.util.*
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolverFacade
import org.cangnova.cangjie.resolve.qualified.resolveQualifierAsReceiverInExpression
import org.cangnova.cangjie.resolve.qualified.resolveQualifierAsStandaloneExpression
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.DataFlowAnalyzer
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.types.makeOption
import org.cangnova.cangjie.types.toFunctionType

// 枚举的解析没有正确实现，enum ab{
//A|B
//}
//func a() :Unit{
//    //这里也是一样，ab是枚举，但是B是枚举的一个构造器，ab.B可以使用，单独使用B也是可以的  ，这里错误的报告了 [UNRESOLVED_REFERENCE] Reference not found: REFERENCE_EXPRESSION [OVERLOAD_RESOLUTION_AMBIGUITY] Overload resolution ambiguity:[org.cangnova.cangjie.resolve.calls.tower.MutableResolvedCallImpl@4938fe68, org.cangnova.cangjie.resolve.calls.tower.MutableResolvedCallImpl@53e36480]
////枚举的表达式解析目前实现没有，需要实现
//    let a:ab = ab.B
//    let a1:ab = ab()
//
//}详细分析CallExpressionResolver
//
//对于 Class<T 可能有类型参数>.Func  或者Enum<T 可能有类型参数>.Constructor   Class和Enum是同一个东西，都应该被正确解析为类型，Func和Constructor是下一阶段解析的，根据仓颉语言规范详细分析重构，仓颉语言文档通过mcp获取，仓颉语言编译器源码在external/cangjie_compiler中
/**
 * 调用表达式解析器
 *
 * 负责解析和类型推断各种形式的调用表达式，包括：
 * - 函数调用：`foo(arg1, arg2)`
 * - 方法调用：`obj.method()`
 * - 构造函数调用：`MyClass()`
 * - 限定表达式：`a.b.c`
 * - 安全调用：`obj?.method()`
 *
 * ## 核心职责
 *
 * 1. **调用解析**：将调用表达式解析为具体的函数或方法描述符
 * 2. **类型推断**：确定调用表达式的结果类型
 * 3. **数据流分析**：追踪调用前后的数据流信息变化
 * 4. **错误诊断**：报告调用相关的错误（如未解析的引用、类型不匹配等）
 *
 * ## 解析策略
 *
 * 对于简单名称表达式，采用多阶段解析策略：
 * 1. 首先尝试作为变量解析
 * 2. 如果失败，尝试作为函数解析
 * 3. 如果仍失败，尝试作为限定符解析
 *
 * ## 安全调用处理
 *
 * 安全调用（`?.`）会：
 * - 在接收者非空时正常调用
 * - 在接收者为空时返回 `Option.None`
 * - 结果类型总是可选类型 `Option<T>`
 *
 * ## 数据流追踪
 *
 * 解析器维护数据流信息，用于：
 * - 智能类型转换（Smart Casts）
 * - 空值分析
 * - 可达性分析
 *
 * @property callResolver 核心调用解析器，处理重载解析
 * @property constantExpressionEvaluator 常量表达式求值器
 * @property argumentTypeResolver 参数类型解析器
 * @property dataFlowAnalyzer 数据流分析器
 * @property builtIns 内置类型定义
 * @property qualifiedExpressionResolver 限定表达式解析器
 * @property languageVersionSettings 语言版本设置
 * @property dataFlowValueFactory 数据流值工厂
 * @property cangjieTypeRefiner 类型精化器
 *
 * @see CallResolver 调用解析核心
 * @see DataFlowAnalyzer 数据流分析
 * @see QualifiedExpressionResolverFacade 限定表达式解析
 */
class CallExpressionResolver(
    private val callResolver: CallResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val builtIns: CangJieBuiltIns,
    private val qualifiedExpressionResolver: QualifiedExpressionResolverFacade,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val cangjieTypeRefiner: CangJieTypeRefiner
) {
    /**
     * 注入表达式类型服务
     *
     * 使用 setter 注入解决组件依赖循环问题。
     * `ExpressionTypingServices` 和 `CallExpressionResolver` 之间存在双向依赖，
     * 通过延迟注入打破循环。
     *
     * @param expressionTypingServices 表达式类型服务
     */
    @set:Inject
    lateinit var expressionTypingServices: ExpressionTypingServices


    /**
     * 获取调用表达式的类型信息（不含最终类型检查）
     *
     * 解析调用表达式并确定其结果类型和数据流信息。
     * 采用多阶段解析策略：
     * 1. 首先尝试作为函数调用解析
     * 2. 如果失败且被调用者是简单名称，尝试作为变量解析（检查是否意外调用了非函数值）
     *
     * @param callExpression 调用表达式 PSI 节点
     * @param receiver 接收者（如 `a.foo()` 中的 `a`），可为 null
     * @param callOperationNode 调用操作符节点（`.` 或 `?.`）
     * @param context 表达式类型上下文
     * @param initialDataFlowInfoForArguments 参数的初始数据流信息
     * @return 包含类型和数据流信息的 [CangJieTypeInfo]
     */
    private fun getCallExpressionTypeInfoWithoutFinalTypeCheck(
        callExpression: CjCallExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo {
        val call = CallMaker.makeCall(receiver, callOperationNode, callExpression)


        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolveName as function call", callExpression
        )
//        函数是一级公民
        val (resolveResult, resolvedCall) = getResolvedCallForFunction(
            call,
            context.replaceTraceAndCache(temporaryForFunction),
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            initialDataFlowInfoForArguments
        )
        if (resolveResult) {
            val functionDescriptor = resolvedCall?.resultingDescriptor
            temporaryForFunction.commit()


            if (callExpression.valueArgumentList == null && callExpression.lambdaArguments.isEmpty()) {
                // 只有类型参数，没有值参数
                val hasValueParameters = functionDescriptor == null || functionDescriptor.valueParameters.isNotEmpty()
                context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters))
            }

            if (functionDescriptor == null) {
                return noTypeInfo(context)
            }
            if (functionDescriptor is ConstructorDescriptor) {
                val constructedClass = functionDescriptor.constructedClass

                if (DescriptorUtils.isSealedClass(constructedClass)) {
                    context.trace.report(SEALED_CLASS_CONSTRUCTOR_CALL.on(callExpression))
                }
            }

            val type = functionDescriptor.returnType
            // 从参数中提取跳出可能性和跳出点数据流信息（如有）
            val arguments = callExpression.valueArguments
            val resultFlowInfo = resolvedCall.dataFlowInfoForArguments.resultInfo
            var jumpFlowInfo = resultFlowInfo
            var jumpOutPossible = false
            for (argument in arguments) {
                val argTypeInfo =
                    context.trace[BindingContext.EXPRESSION_TYPE_INFO, argument.getArgumentExpression()!!]
                if (argTypeInfo != null && argTypeInfo.jumpOutPossible) {
                    jumpOutPossible = true
                    jumpFlowInfo = argTypeInfo.jumpFlowInfo
                    break
                }
            }
            return createTypeInfo(type, resultFlowInfo, jumpOutPossible, jumpFlowInfo)
        }


        val calleeExpression = callExpression.calleeExpression
        if (calleeExpression is CjSimpleNameExpression && callExpression.typeArgumentList == null) {
            val temporaryForVariable = TemporaryTraceAndCache.create(
                context, "trace to resolveName as variable with 'invoke' call", callExpression
            )
            val (notNothing, type) = getVariableType(
                calleeExpression, receiver, callOperationNode,
                context.replaceTraceAndCache(temporaryForVariable)
            )
            val qualifier = temporaryForVariable.trace[BindingContext.QUALIFIER, calleeExpression]
            if (notNothing && (qualifier == null || qualifier !is PackageQualifier)) {

                // 将属性调用标记为不成功，以避免异常
                callExpression.getResolvedCall(temporaryForVariable.trace.bindingContext).let {
                    (it as? MutableResolvedCallImpl)?.addStatus(ResolutionStatus.OTHER_ERROR)
                }

                temporaryForVariable.commit()
                context.trace.report(
                    FUNCTION_EXPECTED.on(
                        calleeExpression, calleeExpression,
                        type ?: ErrorUtils.createErrorType(ErrorTypeKind.ERROR_EXPECTED_TYPE)
                    )
                )
                argumentTypeResolver.analyzeArgumentsAndRecordTypes(
                    BasicCallResolutionContext.create(
                        context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        DataFlowInfoForArgumentsImpl(initialDataFlowInfoForArguments, call)
                    ),
                    ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
                )
                return noTypeInfo(context)
            }
        }
        temporaryForFunction.commit()
        return noTypeInfo(context)
    }

    /**
     * 获取调用表达式的类型信息
     *
     * 这是调用表达式类型推断的主入口点。解析调用并在独立上下文中执行最终类型检查。
     *
     * @param callExpression 调用表达式
     * @param context 表达式类型上下文
     * @return 调用表达式的类型信息
     */
    fun getCallExpressionTypeInfo(
        callExpression: CjCallExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val typeInfo = getCallExpressionTypeInfoWithoutFinalTypeCheck(
            callExpression, null, null, context, context.dataFlowInfo
        )
        if (context.contextDependency == ContextDependency.INDEPENDENT) {
            dataFlowAnalyzer.checkType(typeInfo.type, callExpression, context)
        }
        return typeInfo
    }


    /**
     * 获取简单名称表达式的类型信息
     *
     * 使用当前数据流信息解析简单名称表达式。
     *
     * @param nameExpression 简单名称表达式
     * @param receiver 接收者，可为 null
     * @param callOperationNode 调用操作符节点
     * @param context 表达式类型上下文
     * @return 名称表达式的类型信息
     */
    fun getSimpleNameExpressionTypeInfo(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext
    ) = getSimpleNameExpressionTypeInfo(nameExpression, receiver, callOperationNode, context, context.dataFlowInfo)

    /**
     * 获取变量类型
     *
     * 尝试将名称表达式解析为局部变量或属性，并返回其类型。
     *
     * @param nameExpression 简单名称表达式
     * @param receiver 接收者，可为 null
     * @param callOperationNode 调用操作符节点
     * @param context 表达式类型上下文
     * @return 一对值：(是否找到变量, 变量类型)
     */
    private fun getVariableType(
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext
    ): Pair<Boolean, CangJieType?> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolveName as local variable or property", nameExpression
        )
        val call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )
        val resolutionResult = callResolver.resolveSimpleVariable(contextForVariable)


        temporaryForVariable.commit()
        return Pair(
            !resolutionResult.isNothing,
            if (resolutionResult.isSingleResult) resolutionResult.resultingDescriptor.returnType else null
        )
    }

    /**
     * 解析限定表达式中的延迟接收者
     *
     * 当限定表达式的接收者是一个限定符时，需要延迟解析以确定它是作为表达式接收者还是类型限定符。
     *
     * @param qualifier 限定符
     * @param selectorExpression 选择器表达式
     * @param context 表达式类型上下文
     */
    private fun resolveDeferredReceiverInQualifiedExpression(
        qualifier: Qualifier,
        selectorExpression: CjExpression?,
        context: ExpressionTypingContext
    ) {
        val calleeExpression = CjPsiUtil.deparenthesize(selectorExpression.getCalleeExpressionIfAny())
        val selectorDescriptor = (calleeExpression as? CjReferenceExpression)?.let {
            context.trace[BindingContext.REFERENCE_TARGET, it]
        }

        resolveQualifierAsReceiverInExpression(qualifier, selectorDescriptor, context)
    }

    /**
     * 获取函数调用的解析结果
     *
     * 尝试将调用解析为函数调用，返回解析是否成功及解析后的调用信息。
     *
     * @param call 调用对象
     * @param context 解析上下文
     * @param checkArguments 参数检查模式
     * @param initialDataFlowInfoForArguments 参数的初始数据流信息
     * @return 一对值：(解析是否成功, 解析后的调用)
     */
    private fun getResolvedCallForFunction(
        call: Call,
        context: ResolutionContext<*>,
        checkArguments: CheckArgumentTypesMode,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): Pair<Boolean, ResolvedCall<FunctionDescriptor>?> {
        val results = callResolver.resolveFunctionCall(
            BasicCallResolutionContext.create(
                context, call, checkArguments, DataFlowInfoForArgumentsImpl(initialDataFlowInfoForArguments, call)
            )
        )

        return if (!results.isNothing) {
            if (call.callElement is CjCallableReference) {
                context.trace.record(IS_FUNC, call.callElement as CjNameReferenceExpression, true)
            }

            Pair(true, OverloadResolutionResultsUtil.getResultingCall(results, context))
        } else
            Pair(false, null)
    }

    /**
     * 获取简单名称表达式的枚举条目类型
     *
     * 尝试将简单名称表达式解析为枚举条目。
     *
     * @param nameExpression 简单名称表达式
     * @param receiver 接收者
     * @param callOperationNode 调用操作符节点
     * @param context 表达式类型上下文
     * @param initialDataFlowInfoForArguments 参数的初始数据流信息
     * @return 枚举条目的描述符，如果不是枚举条目则返回 null
     */
    private fun getSimpleNameExpressionEnumEntryType(
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): DeclarationDescriptor? {


        CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, emptyList())



        return null

    }

    /**
     * 获取简单名称表达式的类型信息（内部实现）
     *
     * 采用四阶段解析策略：
     * 1. **步骤1：变量解析** - 尝试作为变量/属性解析
     * 2. **步骤2：函数解析** - 尝试作为函数解析
     * 3. **步骤3：限定符解析** - 尝试作为类型/包限定符解析
     * 4. **步骤4：枚举构造器解析** - 如果接收者是枚举类型，尝试作为枚举构造器解析
     *
     * @param nameExpression 简单名称表达式
     * @param receiver 接收者
     * @param callOperationNode 调用操作符节点
     * @param context 表达式类型上下文
     * @param initialDataFlowInfoForArguments 参数的初始数据流信息
     * @return 名称表达式的类型信息
     */
    private fun getSimpleNameExpressionTypeInfo(
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo {
        // ============================================================
        // 步骤1: 尝试作为变量解析
        // ============================================================
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolveName as variable", nameExpression
        )
        val variableResult = tryResolveAsVariable(
            temporaryForVariable,  nameExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
        )
        if (variableResult != null) {
            return variableResult
        }

        // ============================================================
        // 步骤2: 尝试作为函数解析
        // ============================================================
        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolveName as function", nameExpression
        )
        val functionResult = tryResolveAsFunction(
            temporaryForFunction,  nameExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
        )
        if (functionResult != null) {
            return functionResult
        }

        // ============================================================
        // 步骤3: 尝试作为 Qualifier 解析（类型/包）
        // ============================================================
        val temporaryForQualifier = TemporaryTraceAndCache.create(
            context, "trace to resolveName as qualifier", nameExpression
        )
        val qualifierResult = tryResolveAsQualifier(
            temporaryForQualifier, nameExpression, receiver, context
        )
        if (qualifierResult != null) {
            return qualifierResult
        }

        // ============================================================
        // 步骤4: 尝试作为枚举构造器解析
        // ============================================================
        val temporaryForEnumConstructor = TemporaryTraceAndCache.create(
            context, "trace to resolveName as enum constructor", nameExpression
        )
        val enumConstructorResult = tryResolveAsEnumConstructor(
            temporaryForEnumConstructor,
            nameExpression,
            receiver,
            callOperationNode,
            context,
            initialDataFlowInfoForArguments
        )
        if (enumConstructorResult != null) {
            return enumConstructorResult
        } else {
//            提交
            temporaryForVariable.commit()
        }

        // 所有解析策略都失败，返回无类型信息
        return noTypeInfo(context)
    }

    /**
     * 步骤1: 尝试将简单名称表达式解析为变量
     *
     * @return 如果成功解析为变量，返回类型信息；否则返回 null
     */
    private fun tryResolveAsVariable(
        temporaryForVariable: TemporaryTraceAndCache,
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo? {

        val (notNothing, type) = getVariableType(
            nameExpression, receiver, callOperationNode,
            context.replaceTraceAndCache(temporaryForVariable)
        )

        if (notNothing) {
            temporaryForVariable.commit()
            return createTypeInfo(type, initialDataFlowInfoForArguments)
        }

        return null
    }

    /**
     * 步骤2: 尝试将简单名称表达式解析为函数
     *
     * @return 如果成功解析为函数，返回类型信息；否则返回 null
     */
    private fun tryResolveAsFunction(
        temporaryForFunction: TemporaryTraceAndCache,
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo? {
        val call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, emptyList())


        val newContext = context.replaceTraceAndCache(temporaryForFunction)
        val (resolveResult, resolvedCall) = getResolvedCallForFunction(
            call, newContext, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, initialDataFlowInfoForArguments
        )

        if (resolveResult) {
            val functionDescriptor = resolvedCall?.resultingDescriptor
            // 排除构造器（构造器在步骤4中处理）
            if (functionDescriptor !is ConstructorDescriptor) {
                temporaryForFunction.commit()
                return createTypeInfo(functionDescriptor?.toFunctionType(), context)
            }
        }

        return null
    }

    /**
     * 步骤3: 尝试将简单名称表达式解析为 Qualifier（类型或包限定符）
     *
     * @return 如果成功解析为 Qualifier，返回类型信息；否则返回 null
     */
    private fun tryResolveAsQualifier(
        trace: TemporaryTraceAndCache,
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        context: ExpressionTypingContext
    ): CangJieTypeInfo? {

        val contextForQualifier = context.replaceTraceAndCache(trace)

        val qualifier = qualifiedExpressionResolver.resolveNameExpressionAsQualifierForDiagnostics(
            nameExpression,
            receiver,
            contextForQualifier
        )

        if (qualifier != null) {
            resolveQualifierAsStandaloneExpression(qualifier, contextForQualifier)
            trace.commit()
            return noTypeInfo(context)
        }

        return null
    }

    /**
     * 步骤4: 尝试将简单名称表达式解析为枚举构造器
     *
     * 当接收者是枚举类型时，尝试将名称解析为该枚举的构造器（枚举条目）。
     *
     * @return 如果成功解析为枚举构造器，返回类型信息；否则返回 null
     */
    private fun tryResolveAsEnumConstructor(
        trace: TemporaryTraceAndCache,
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo? {


        val call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, emptyList())
        val contextForEnum = context.replaceTraceAndCache(trace)

        val results = callResolver.resolveEnumCall(
            trace,
            BasicCallResolutionContext.create(
                contextForEnum, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(initialDataFlowInfoForArguments, call)
            )
        )

        if (!results.isNothing) {
            trace.commit()
            val descriptor = results.resultingDescriptor
            return createTypeInfo(descriptor?.returnType, initialDataFlowInfoForArguments)
        }


        // 检查接收者是否为枚举 Qualifier
//        val qualifier = (nameExpression.parent as? CjExpression)?.let { context.trace[BindingContext.QUALIFIER, it] }
//        if (qualifier is ClassifierQualifier) {
//            val classDescriptor = when (qualifier) {
//                is ClassQualifier -> qualifier.descriptor
//                is TypeAliasQualifier -> qualifier.classDescriptor
//                else -> null
//            }
//
//            // 如果是枚举类型，尝试解析枚举构造器
//            if (classDescriptor != null && DescriptorUtils.isEnum(classDescriptor)) {
//                val call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, emptyList())
//                val contextForEnum = context.replaceTraceAndCache(temporaryForEnumConstructor)
//
//                val results = callResolver.resolveEnumCall(
//                    temporaryForEnumConstructor,
//                    BasicCallResolutionContext.create(
//                        contextForEnum, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
//                        DataFlowInfoForArgumentsImpl(initialDataFlowInfoForArguments, call)
//                    )
//                )
//
//                if (!results.isNothing) {
//                    temporaryForEnumConstructor.commit()
//                    val descriptor = results.resultingDescriptor
//                    return createTypeInfo(descriptor?.returnType, initialDataFlowInfoForArguments)
//                }
//            }
//        }

        return null
    }

    /**
     * 解析简单名称表达式
     *
     * 将简单名称表达式解析为变量描述符。
     *
     * @param context 表达式类型上下文
     * @param expression 简单名称表达式
     * @param traceAndCache 临时追踪和缓存
     * @return 变量解析结果
     */
    private fun resolveSimpleName(
        context: ExpressionTypingContext, expression: CjSimpleNameExpression, traceAndCache: TemporaryTraceAndCache
    ): OverloadResolutionResults<VariableDescriptor> {
        val call = CallMaker.makePropertyCall(null, null, expression)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(traceAndCache), call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )
        return callResolver.resolveSimpleVariable(contextForVariable)
    }

    /**
     * 将限定表达式展开为元素链
     *
     * 解析限定表达式中的每个元素，生成接收者-选择器对的链表。
     * 用于逐步处理链式调用如 `a.b.c.d`。
     *
     * @receiver 限定表达式
     * @param context 表达式类型上下文
     * @return 调用表达式元素链
     */
    private fun CjQualifiedExpression.elementChain(context: ExpressionTypingContext) =
        qualifiedExpressionResolver.resolveQualifierInExpressionAndUnroll(this, context) { nameExpression ->
            val temporaryTraceAndCache =
                TemporaryTraceAndCache.create(
                    context,
                    "trace to resolveName as local variable or property",
                    nameExpression
                )
            val resolutionResult = resolveSimpleName(context, nameExpression, temporaryTraceAndCache)

            // 枚举构造器通过 EnumConstructorDescriptor 直接暴露
            when (resolutionResult.resultCode) {
                OverloadResolutionResults.Code.NAME_NOT_FOUND, OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER -> false
                else -> {

                    val success = resolutionResult.isSuccess
                    if (success) {
                        temporaryTraceAndCache.commit()
                    }
                    success
                }
            }
        }


    /**
     * 获取限定表达式的类型信息
     *
     * 处理形如 `x.y` 或 `x?.z` 的限定表达式，控制数据流信息的变化。
     *
     * ## 处理流程
     *
     * 1. 将限定表达式展开为元素链
     * 2. 解析第一个接收者的类型
     * 3. 逐步处理每个选择器，更新类型和数据流信息
     * 4. 处理安全调用的空值传播
     *
     * ## 安全调用处理
     *
     * 对于安全调用（`?.`）：
     * - 如果接收者非空，正常调用选择器
     * - 如果接收者为空，整个表达式返回 `Option.None`
     * - 结果类型会被包装为 `Option<T>`
     *
     * @param expression 限定表达式
     * @param context 表达式类型上下文
     * @return 限定表达式的类型信息，包含类型和数据流信息
     */
    fun getQualifiedExpressionTypeInfo(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val currentContext =
            context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(ContextDependency.INDEPENDENT)
        val trace = currentContext.trace

        val elementChain = expression.elementChain(currentContext)
        val firstReceiver = elementChain.first().receiver

        // 统一处理所有限定符类型
        // 对于 ClassifierQualifier（包括普通类和枚举类），不需要获取类型信息
        // 成员访问通过 staticScope 解析
        var receiverTypeInfo = when (val qualifier = trace[BindingContext.QUALIFIER, firstReceiver]) {
            null -> expressionTypingServices.getTypeInfo(firstReceiver, currentContext)
            else -> CangJieTypeInfo(null, currentContext.dataFlowInfo)
        }

        var resultTypeInfo = receiverTypeInfo

        var allUnsafe = true
        // 分支点：在第一个安全调用之前
        var branchPointDataFlowInfo = receiverTypeInfo.dataFlowInfo

        for (element in elementChain) {
            val receiverType = receiverTypeInfo.type
                ?: ErrorUtils.createErrorType(
                    ErrorTypeKind.ERROR_RECEIVER_TYPE,
                    when (val receiver = element.receiver) {
                        is CjNameReferenceExpression -> receiver.referencedName
                        else -> receiver.text
                    }
                )

            val receiver = trace[BindingContext.QUALIFIER, element.receiver]
                ?: ExpressionReceiver.create(element.receiver, receiverType, trace.bindingContext)

            val qualifiedExpression = element.qualified
            val lastStage = qualifiedExpression === expression
            // 在最后阶段移除 NO_EXPECTED_TYPE / INDEPENDENT
            val contextForSelector = (if (lastStage) context else currentContext).replaceDataFlowInfo(
                if (receiver is ReceiverValue && TypeUtils.isOptionType(receiver.type) && !element.safe) {
                    // 带有可空接收者的调用：从分支点获取数据流信息
                    branchPointDataFlowInfo
                } else {
                    // 从当前接收者获取数据流信息
                    receiverTypeInfo.dataFlowInfo
                }
            )

            val selectorTypeInfo = getSafeOrUnsafeSelectorTypeInfo(receiver, element, contextForSelector)
            // 如果只有点号而没有 ?.，则继续移动分支点
            allUnsafe = allUnsafe && !element.safe
            if (allUnsafe) {
                branchPointDataFlowInfo = selectorTypeInfo.dataFlowInfo
            }

            resultTypeInfo =
                checkSelectorTypeInfo(qualifiedExpression, selectorTypeInfo, contextForSelector).replaceDataFlowInfo(
                    branchPointDataFlowInfo
                )
            if (!lastStage) {
                recordResultTypeInfo(qualifiedExpression, resultTypeInfo, contextForSelector)
            }
            // 对于下一阶段（如有），当前阶段的选择器将作为接收者！
            receiverTypeInfo = selectorTypeInfo
        }
        return resultTypeInfo
    }

    /**
     * 获取非安全选择器的类型信息
     *
     * 处理普通的点调用（`.`），根据选择器表达式的类型分发到不同的处理方法。
     *
     * @param receiver 接收者
     * @param callOperationNode 调用操作符节点
     * @param selectorExpression 选择器表达式
     * @param context 表达式类型上下文
     * @param initialDataFlowInfoForArguments 参数的初始数据流信息
     * @return 选择器的类型信息
     */
    private fun getUnsafeSelectorTypeInfo(
        receiver: Receiver,
        callOperationNode: ASTNode?,
        selectorExpression: CjExpression?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo = when (selectorExpression) {
        is CjCallExpression -> getCallExpressionTypeInfoWithoutFinalTypeCheck(
            selectorExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
        )

        is CjSimpleNameExpression -> getSimpleNameExpressionTypeInfo(
            selectorExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
        )

        is CjExpression -> {
            expressionTypingServices.getTypeInfo(selectorExpression, context)
            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression))
            noTypeInfo(context)
        }

        else /*null*/ -> noTypeInfo(context)
    }

    /**
     * 获取安全或非安全选择器的类型信息
     *
     * 根据调用是安全调用（`?.`）还是普通调用（`.`）来处理选择器。
     *
     * ## 仓颉语言的 `?.` 语义
     *
     * `?.` 是 **Option 类型的语法糖**,不是运行时 null 检查:
     * - **仅适用于 `Option<T>` 类型的接收者**
     * - 如果接收者不是 Option 类型,应报告编译错误
     * - 结果类型始终为 `Option<U>` (U 是选择器的结果类型)
     *
     * 示例:
     * ```cangjie
     * let x: Option<Foo> = Some(Foo())
     * let y = x?.bar  // 类型: Option<BarType>
     *
     * let z: Foo = Foo()
     * let w = z?.bar  // 编译错误: 不能在非 Option 类型上使用 ?.
     * ```
     *
     * @param receiver 接收者
     * @param element 调用表达式元素
     * @param context 表达式类型上下文
     * @return 选择器的类型信息
     */
    private fun getSafeOrUnsafeSelectorTypeInfo(
        receiver: Receiver,
        element: CallExpressionElement,
        context: ExpressionTypingContext
    ):
            CangJieTypeInfo {
        val initialDataFlowInfoForArguments = context.dataFlowInfo

        val callOperationNode =
            AstLoadingFilter.forceAllowTreeLoading(element.qualified.containingFile, ThrowableComputable {
                element.node
            })

        // 仓颉语言: 检查 ?. 操作符的使用是否正确
        if (element.safe && receiver is ReceiverValue) {
            val receiverType = receiver.type
            val isReceiverOption = TypeUtils.isOptionType(receiverType)

            if (!isReceiverOption) {
                // 仓颉语言: ?. 只能用于 Option<T> 类型
                // 这是编译错误,不是警告
                reportUnnecessarySafeCall(
                    context.trace,
                    receiverType,
                    element.qualified,
                    callOperationNode,
                    receiver,
                    context.languageVersionSettings
                )
            }
        }

        val selector = element.selector

        var selectorTypeInfo =
            getUnsafeSelectorTypeInfo(receiver, callOperationNode, selector, context, initialDataFlowInfoForArguments)
                .run {
                    val type = type ?: return@run this
                    replaceType(cangjieTypeRefiner.refineType(type))
                }

        if (receiver is Qualifier) {
            resolveDeferredReceiverInQualifiedExpression(receiver, selector, context)
        }

        val selectorType = selectorTypeInfo.type
        if (selectorType != null) {
            // 仓颉语言: 如果使用 ?. 操作符,结果类型总是 Option<T>
            if (element.safe) {
                selectorTypeInfo = selectorTypeInfo.replaceType(selectorType.makeOption())
            }
            if (selector != null) {
                context.trace.recordType(selector, selectorTypeInfo.type)
            }
        }
        return selectorTypeInfo
    }

    /**
     * 记录结果类型信息
     *
     * 将限定表达式的类型信息记录到绑定追踪中，避免重复处理。
     *
     * @param qualified 限定表达式
     * @param resultTypeInfo 结果类型信息
     * @param context 表达式类型上下文
     */
    private fun recordResultTypeInfo(
        qualified: CjQualifiedExpression,
        resultTypeInfo: CangJieTypeInfo,
        context: ExpressionTypingContext
    ) {
        val trace = context.trace
        if (trace[BindingContext.PROCESSED, qualified] != true) {
            // 存储类型信息（以防止调用补全器出现问题）
            trace.record(BindingContext.PROCESSED, qualified)
            trace.record(BindingContext.EXPRESSION_TYPE_INFO, qualified, resultTypeInfo)
            // 在分析之前保存作用域并修复调试器：参见 CodeFragmentAnalyzer.correctContextForExpression
            trace.recordScope(context.scope, qualified)
            context.replaceDataFlowInfo(resultTypeInfo.dataFlowInfo).recordDataFlowInfo(qualified)
        }
    }

    /**
     * 检查选择器类型信息
     *
     * 对选择器的类型信息进行最终检查，处理常量表达式求值和类型检查。
     *
     * @param qualified 限定表达式
     * @param selectorTypeInfo 选择器类型信息
     * @param context 表达式类型上下文
     * @return 检查后的类型信息
     */
    private fun checkSelectorTypeInfo(
        qualified: CjQualifiedExpression,
        selectorTypeInfo: CangJieTypeInfo,
        context: ExpressionTypingContext
    ):
            CangJieTypeInfo {
        val value = constantExpressionEvaluator.evaluateExpression(qualified, context.trace, context.expectedType)
        return if (value != null && value.isPure) {
            dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, qualified, context)
        } else {
            if (context.contextDependency == ContextDependency.INDEPENDENT) {
                dataFlowAnalyzer.checkType(selectorTypeInfo.type, qualified, context)
            }
            selectorTypeInfo
        }
    }

    companion object {

        /**
         * 报告不必要的安全调用警告
         *
         * 当接收者类型不可能为空时，安全调用是不必要的，应该使用普通调用。
         *
         * @param trace 绑定追踪
         * @param type 接收者类型
         * @param callElement 调用元素
         * @param callOperationNode 调用操作符节点
         * @param explicitReceiver 显式接收者
         * @param languageVersionSettings 语言版本设置
         */
        fun reportUnnecessarySafeCall(
            trace: BindingTrace,
            type: CangJieType,
            callElement: CjQualifiedExpression,
            callOperationNode: ASTNode,
            explicitReceiver: Receiver?,
            languageVersionSettings: LanguageVersionSettings
        ) {
            if (explicitReceiver is ExpressionReceiver && explicitReceiver.expression is CjSuperExpression) {
                trace.report(UNEXPECTED_SAFE_CALL.on(callOperationNode.psi))
            } else if (!type.isError) {
                trace.report(UNNECESSARY_SAFE_CALL.on(callOperationNode.psi, type))
                // 不再报告 SAFE_CALL_WILL_CHANGE_NULLABILITY 警告，因为默认启用了 SafeCallsAreAlwaysNullable
            }
        }

    }

}


