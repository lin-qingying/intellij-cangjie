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

package org.cangnova.cangjie.resolve.calls

import com.intellij.psi.PsiElement
import jakarta.inject.Inject
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED_IN_ENUM
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.GET
import org.cangnova.cangjie.name.OperatorNameConventions.SET
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus.checkCanceled
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.DescriptorUtils.getSuperClassType
import org.cangnova.cangjie.resolve.ModifierCheckerCore.check
import org.cangnova.cangjie.resolve.TypeResolver
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.TemporaryBindingTrace
import org.cangnova.cangjie.resolve.binding.recordScope
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.components.InferenceSession.Companion.default
import org.cangnova.cangjie.resolve.calls.context.*
import org.cangnova.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.tasks.*
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategyImpl.Companion.create
import org.cangnova.cangjie.resolve.calls.tower.NewResolutionOldInference
import org.cangnova.cangjie.resolve.calls.tower.PSICallResolver
import org.cangnova.cangjie.resolve.calls.util.*
import org.cangnova.cangjie.resolve.calls.util.CallMaker.makeCall
import org.cangnova.cangjie.resolve.getSuperClassOrAny
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil.forceResolveAllContents
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.SyntheticScopes
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver.Companion.create
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.createFunctionType
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.ExpressionTypingVisitorDispatcher
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.utils.PerformanceCounter.Companion.create

/**
 * 调用解析器
 *
 * 这是仓颉语言调用解析的核心类，负责解析各种类型的函数调用、构造器调用、操作符调用等。
 * 它协调了重载解析、类型推断、候选筛选等多个解析阶段。
 *
 * 主要功能：
 * - **函数调用解析**: 解析普通函数调用、扩展函数调用、操作符调用
 * - **构造器解析**: 解析类构造器调用和委托调用
 * - **重载解析**: 在多个候选中选择最匹配的函数
 * - **类型推断**: 推断泛型类型参数和返回类型
 * - **变量解析**: 解析变量引用和属性访问
 *
 * 解析流程：
 * 1. 创建调用上下文 (BasicCallResolutionContext)
 * 2. 收集候选描述符 (通过 scope 或显式提供)
 * 3. 执行重载解析 (通过 NewResolutionOldInference 或 PSICallResolver)
 * 4. 应用类型推断和约束求解
 * 5. 返回解析结果 (OverloadResolutionResults)
 *
 * 依赖注入：
 * - [expressionTypingServices]: 表达式类型服务
 * - [syntheticScopes]: 合成作用域（用于扩展函数等）
 * - [argumentTypeResolver]: 参数类型解析器
 * - [newResolutionOldInference]: 旧版推断引擎
 * - [psiCallResolver]: PSI 调用解析器（新版推断引擎）
 * - [typeResolver]: 类型解析器
 *
 * 性能优化：
 * - 使用性能计数器追踪解析耗时
 * - 支持缓存解析结果
 * - 使用临时追踪避免污染全局上下文
 *
 * @property builtIns 内置类型系统
 * @property languageVersionSettings 语言版本配置
 * @property dataFlowValueFactory 数据流值工厂
 *
 * @see BasicCallResolutionContext
 * @see OverloadResolutionResults
 * @see NewResolutionOldInference
 * @see PSICallResolver
 */
class CallResolver(
    private val builtIns: CangJieBuiltIns,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
) {
    @set:Inject
    private lateinit var expressionTypingServices: ExpressionTypingServices
    @set:Inject
    private lateinit var syntheticScopes: SyntheticScopes
    @set:Inject
    private lateinit var argumentTypeResolver: ArgumentTypeResolver
    @set:Inject
    private lateinit var newResolutionOldInference: NewResolutionOldInference
    @set:Inject
    private lateinit var psiCallResolver: PSICallResolver

    @set:Inject
    lateinit var typeResolver: TypeResolver


    /**
     * 计算任务并解析调用
     *
     *
     * 此方法用于在给定的上下文中计算任务并解析特定名称的调用它接受一个基本的调用解析上下文、一个名称、一个引用表达式和一个解析类型，
     * 并返回一个OverloadResolutionResults的实例，该实例包含解析的结果和任务
     *
     * @param context             调用解析的上下文，包含有关调用的信息和设置
     * @param name                要解析的调用名称
     * @param referenceExpression 一个CjReferenceExpression对象，表示对某个元素的引用
     * @param kind                解析的类型，指示解析应该如何执行
     * @param <D>                 一个扩展了CallableDescriptor的类型，表示可以被解析的调用描述符
     * @return 返回一个OverloadResolutionResults的实例，包含解析的结果和任务
    </D> */
    fun <D : CallableDescriptor> computeTasksAndResolveCall(
        context: BasicCallResolutionContext,
        name: Name,
        referenceExpression: CjReferenceExpression,
        kind: NewResolutionOldInference.ResolutionKind
    ): OverloadResolutionResults<D> {
        // 创建一个追踪策略，用于在解析过程中记录和追踪信息
        val tracing = create(referenceExpression, context.call)
        // 调用重载的方法，计算任务并解析调用
        return computeTasksAndResolveCall(context, name, tracing, kind)
    }

    private fun <D : CallableDescriptor> checkArgumentTypesAndFail(context: BasicCallResolutionContext): OverloadResolutionResultsImpl<D> {
        argumentTypeResolver.checkTypesWithNoCallee(context)
        return OverloadResolutionResultsImpl.nameNotFound()
    }

    /**
     * 解析具有给定名称的函数调用
     *
     * 根据提供的函数名称解析调用。这是最常用的函数调用解析入口点之一。
     *
     * @param context 解析上下文，包含作用域、数据流信息等
     * @param call 调用对象，包含调用表达式、参数等信息
     * @param functionReference 函数引用表达式，用于追踪和报告
     * @param name 要解析的函数名称
     * @return 重载解析结果，包含所有匹配的候选和最终选择的描述符
     */
    fun resolveCallWithGivenName(
        context: ResolutionContext<*>,
        call: Call,
        functionReference: CjReferenceExpression,
        name: Name
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        return computeTasksAndResolveCall(
            callResolutionContext, name, functionReference,
            NewResolutionOldInference.ResolutionKind.Function
        )
    }

    /**
     * 解析具有给定名称的函数调用（带自定义追踪策略）
     *
     * 与 [resolveCallWithGivenName] 类似，但允许自定义追踪策略。
     * 追踪策略用于绑定调用和记录诊断信息。
     *
     * @param context 解析上下文
     * @param call 调用对象
     * @param name 函数名称
     * @param tracing 追踪策略，用于绑定调用和记录错误
     * @return 重载解析结果
     */
    fun resolveCallWithGivenName(
        context: ResolutionContext<*>,
        call: Call,
        name: Name,
        tracing: TracingStrategy
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        return computeTasksAndResolveCall(
            callResolutionContext,
            name,
            tracing,
            NewResolutionOldInference.ResolutionKind.Function
        )
    }

    /**
     * 解析具有已知候选的函数调用
     *
     * 当已经知道候选函数时使用此方法进行解析。
     * 通常用于二次解析或特殊场景。
     *
     * @param call 调用对象
     * @param tracing 追踪策略
     * @param context 解析上下文
     * @param candidate 已知的候选描述符
     * @param dataFlowInfoForArguments 参数的数据流信息（可选）
     * @return 重载解析结果
     */
    fun resolveCallWithKnownCandidate(
        call: Call,
        tracing: TracingStrategy,
        context: ResolutionContext<*>,
        candidate: OldResolutionCandidate<FunctionDescriptor>,
        dataFlowInfoForArguments: MutableDataFlowInfoForArguments?
    ): OverloadResolutionResults<FunctionDescriptor> {
        return callResolvePerfCounter.time {
            val basicCallResolutionContext =
                BasicCallResolutionContext.create(
                    context,
                    call,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    dataFlowInfoForArguments
                )
            val candidates = setOf(candidate)

            val resolutionTask = ResolutionTask(
                NewResolutionOldInference.ResolutionKind.GivenCandidates(), null, candidates
            )
            doResolveCallOrGetCachedResults(basicCallResolutionContext, resolutionTask, tracing)
        }
    }

    fun resolveCall(
        context: ExpressionTypingContext,
        expression: CjCallExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }
        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(expression, call)
        )
    }

    /**
     * 解析二元操作符调用
     *
     * 二元操作符（如 +, -, *, / 等）会被解析为对应的函数调用。
     * 例如 `a + b` 会被解析为 `a.plus(b)` 的调用。
     *
     * @param context 表达式类型上下文
     * @param receiver 接收者表达式（操作符左侧）
     * @param binaryExpression 二元表达式
     * @param name 操作符对应的函数名（如 plus, minus）
     * @return 重载解析结果
     */
    fun resolveBinaryCall(
        context: ExpressionTypingContext,
        receiver: ExpressionReceiver,
        binaryExpression: CjBinaryExpression,
        name: Name
    ): OverloadResolutionResults<FunctionDescriptor> {
        return resolveCallWithGivenName(
            context,
            makeCall(receiver, binaryExpression),
            binaryExpression.operationReference,
            name
        )
    }

    fun resolveBinaryCall(
        context: ExpressionTypingContext,
        call: Call,
        binaryExpression: CjBinaryExpression,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {

        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(binaryExpression.operationReference, call)
        )
    }

    fun resolveBinaryCall(
        context: ExpressionTypingContext,
        receiver: ExpressionReceiver,
        binaryExpression: CjBinaryExpression,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val call = makeCall(receiver, binaryExpression)
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(binaryExpression.operationReference, call)
        )
    }

    fun resolveRangeLiteralCallWithGivenDescriptor(
        context: ExpressionTypingContext,
        expression: CjRangeExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(expression, call)
        )
    }

    fun resolveBloackReturnCallWithGivenDescriptor(
        context: ExpressionTypingContext,
        expression: CjBlockExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, TracingStrategyBlockExpression(expression, call)
        )
    }

    fun resolveCallExpressionWithGivenDescriptor(
        context: ExpressionTypingContext,
        expression: CjCallExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(expression, call)
        )
    }

    fun resolveCollectionLiteralCallWithGivenDescriptor(
        context: ExpressionTypingContext,
        expression: CjCollectionLiteralExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(expression, call)
        )
    }


    private fun resolveConstructorDelegationCall(
        context: BasicCallResolutionContext,
        call: CjConstructorDelegationCall,
        calleeExpression: CjConstructorDelegationReferenceExpression,
        currentClassDescriptor: ClassDescriptor
    ): OverloadResolutionResults<ConstructorDescriptor> {
        var context = context
        context.trace.record(BindingContext.LEXICAL_SCOPE, call, context.scope)

        val isThisCall = calleeExpression.isThis /*|| currentClassDescriptor.getKind() == ClassKind.STRUCT*/
        if (currentClassDescriptor.kind == ClassKind.ENUM && !isThisCall) {
            context.trace.report(DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR.on(calleeExpression))
            return checkArgumentTypesAndFail(context)
        }

        val delegateClassDescriptor =
            if (isThisCall) currentClassDescriptor else currentClassDescriptor.getSuperClassOrAny()
        val constructors = delegateClassDescriptor.constructors

        //        if (!isThisCall && currentClassDescriptor.getUnsubstitutedPrimaryConstructor() != null) {
//            if (DescriptorUtils.canHaveDeclaredConstructors(currentClassDescriptor)) {
//                // Diagnostic is meaningless when reporting on interfaces and object
//                PsiElement reportOn = calcReportOn(calleeExpression);
//                context.trace.report(PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.on(reportOn));
//            }
//            if (call.isImplicit()) return OverloadResolutionResultsImpl.nameNotFound();
//        }
        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(context.call.getValueArgumentListOrElement()))
            return checkArgumentTypesAndFail(context)
        }


        val superType =
            if (isThisCall) currentClassDescriptor.defaultType else getSuperClassType(currentClassDescriptor)

        val candidatesAndContext =
            prepareCandidatesAndContextForConstructorCall(
                superType, context,
                syntheticScopes
            )
        val candidates = candidatesAndContext.first
        context = candidatesAndContext.second

        val tracing =
            if (call.isImplicit) TracingStrategyForImplicitConstructorDelegationCall(call, context.call) else create(
                calleeExpression,
                context.call
            )

        if (call.isImplicit) call else calleeExpression

        //
//        if (delegateClassDescriptor.isInner()
//                && !DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, reportOn,
//                (ClassDescriptor) delegateClassDescriptor.getContainingDeclaration())) {
//            return checkArgumentTypesAndFail(context);
//        }
        return computeTasksFromCandidatesAndResolvedCall(context, candidates, tracing)
    }

    private fun <D : FunctionDescriptor> computeTasksFromCandidatesAndResolvedCall(
        context: BasicCallResolutionContext,
        candidates: Collection<OldResolutionCandidate<D>>,
        tracing: TracingStrategy
    ): OverloadResolutionResults<D> {
        return callResolvePerfCounter.time {
            val resolutionTask = ResolutionTask(
                NewResolutionOldInference.ResolutionKind.GivenCandidates(), null, candidates
            )
            doResolveCallOrGetCachedResults(context, resolutionTask, tracing)
        }
    }

    private fun <D : FunctionDescriptor> computeTasksFromCandidatesAndResolvedCall(
        context: BasicCallResolutionContext,
        referenceExpression: CjReferenceExpression,
        candidates: Collection<OldResolutionCandidate<D>>
    ): OverloadResolutionResults<D> {
        return computeTasksFromCandidatesAndResolvedCall(
            context, candidates,
            create(referenceExpression, context.call)
        )
    }

    /**
     * 解析枚举调用
     *
     * 解析枚举成员的访问和枚举构造器调用。
     * 枚举调用具有特殊的解析语义，因为枚举成员可以是值或带构造器的类型。
     *
     * 处理两种场景：
     * 1. 简单枚举成员访问：`Color.Red`
     * 2. 带参数的枚举构造：`Color.Red(255)`
     *
     * @param tcache 临时追踪和缓存
     * @param context 调用解析上下文
     * @param kind 解析类型，默认为 EnumEntry
     * @return 重载解析结果
     */
    fun resolveEnumCall(

        tcache: TemporaryTraceAndCache,

        context: BasicCallResolutionContext,
        kind: NewResolutionOldInference.ResolutionKind = NewResolutionOldInference.ResolutionKind.EnumEntry
    ): OverloadResolutionResults<out CallableDescriptor> {
        checkCanceled()


        when (val calleeExpression = context.call.calleeExpression) {
            is CjSimpleNameExpression -> {
                return computeTasksAndResolveCall(
                    context, calleeExpression.referencedNameAsName, calleeExpression,
                    kind
                )
            }

            null -> {
                return checkArgumentTypesAndFail(context)
            }

            else -> {
                val expectedType: CangJieType = NO_EXPECTED_TYPE

                val calleeType = expressionTypingServices.safeGetType(
                    context.scope,
                    calleeExpression,
                    expectedType,
                    context.dataFlowInfo,
                    context.inferenceSession,
                    context.trace
                )
                val expressionReceiver = create(calleeExpression, calleeType, context.trace.bindingContext)

                val call: Call = CallTransformer.CallForImplicitInvoke(
                    context.call.explicitReceiver, expressionReceiver, context.call,
                    false
                )
                val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, call, calleeType)
                return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
            }
        }
//
//        val calleeExpression = context.call.calleeExpression
//
//        val callExpression = context.call.callElement
//        val dotParent = callExpression.getStrictParentOfType<CjDotQualifiedExpression>()
//        val isCall = callExpression is CjCallExpression && callExpression.valueArgumentList != null
//        var result: OverloadResolutionResults<*>? = null
//
//        fun getResult() {
//            //        TODO    a.b<Int> 这种情况不做处理了，只报错无法推断
//            if (calleeExpression is CjSimpleNameExpression) {
//
//                result = computeTasksAndResolveCall<CallableDescriptor>(
//                    context, calleeExpression.referencedNameAsName, calleeExpression,
//                    kind
//                )
//
//            }
//        }
//
//        getResult()
//
//        if (result is ManyCandidates) {
//            return result!!
//        }
//
//        if (result == null || result!!.isNothing) {
//            return OverloadResolutionResultsImpl.nameNotFound()
//        }
//
//
//        val resultDescriptor: EnumClassCallableDescriptor
//
//        if (result!!.resultingDescriptor == null) {
//            return result!!
//        } else {
//            resultDescriptor = result!!.resultingDescriptor as EnumClassCallableDescriptor
//        }
//
//        if (kind == NewResolutionOldInference.ResolutionKind.CaseEnum) {
//            return result!!
//        }
//
//        //        判断结果是否有无参构造，如果没有则调用 resolveCallForInvoke
//        if (resultDescriptor.hashUnsubstitutedPrimaryConstructor() && !isCall) {
////            有无参构造，并且不是call
//            return result!!
//        } else if (!resultDescriptor.hashUnsubstitutedPrimaryConstructor() && isCall) {
//
//            return result!!
//
//        }
//        val calleeType = resultDescriptor.returnType
//
//        val expressionReceiver = create(
//            calleeExpression!!, calleeType!!, context.trace.bindingContext
//        )
//
//        val call: Call = CallTransformer.CallForImplicitInvoke(
//            context.call.explicitReceiver, expressionReceiver, context.call,
//            false
//        )
//        val tracingForInvoke = TracingStrategyForInvoke(
//            calleeExpression, call, calleeType
//        )
//        if (isCall) {
//            if (resultDescriptor.typeParameters.isNotEmpty()) {
////                如果有类型参数，去掉表达式中的类型参数
////                然而实际上，操作符函数不能有类型参数，可以直接设置为null
//                call.noTypeParameter = true
//            }
//            val temp =
//                resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
//            call.noTypeParameter = false
//
//
//            return temp.replaceCode(OverloadResolutionResults.Code.SUCCESS_NAME_NOT_FOUND)
//        }
//
//        return OverloadResolutionResultsImpl.nameNotFound()
    }

    /**
     * 解析函数调用
     *
     * 这是函数调用解析的主要入口点，支持多种调用类型：
     * - 普通函数调用：`foo(1, 2)`
     * - 数组访问：`arr[0]` (解析为 get/set 操作符)
     * - 构造器调用：`MyClass()`
     * - 构造器委托调用：`this()` 或 `super()`
     * - Lambda 调用：`{ x -> x + 1 }()`
     * - invoke 调用：`obj(1, 2)` (调用 obj.invoke())
     *
     * 根据调用表达式的类型，分发到相应的解析逻辑。
     *
     * @param context 调用解析上下文
     * @return 重载解析结果
     */
    fun resolveFunctionCall(context: BasicCallResolutionContext): OverloadResolutionResults<out FunctionDescriptor> {
        checkCanceled()

        val callType = context.call.callType
        if (callType == Call.CallType.ARRAY_GET_METHOD || callType == Call.CallType.ARRAY_SET_METHOD) {
            val name =
                if (callType == Call.CallType.ARRAY_GET_METHOD) GET else SET
            val arrayAccessExpression =
                context.call.callElement as CjArrayAccessExpression
            return computeTasksAndResolveCall(
                context, name, arrayAccessExpression,
                NewResolutionOldInference.ResolutionKind.Function
            )
        }
        when (val calleeExpression = context.call.calleeExpression) {
            is CjSimpleNameExpression -> {
                if (context.call is CallMaker.CallImpl) {
                    return computeTasksAndResolveCall(
                        context, calleeExpression.referencedNameAsName, calleeExpression,
                        NewResolutionOldInference.ResolutionKind.CallableReference
                    )
                }
                return computeTasksAndResolveCall(
                    context, calleeExpression.referencedNameAsName, calleeExpression,
                    NewResolutionOldInference.ResolutionKind.Function
                )
            }

            is CjConstructorCalleeExpression -> {
                return resolveCallForConstructor(context, calleeExpression)
            }

            is CjConstructorDelegationReferenceExpression -> {
                val delegationCall = context.call.callElement as CjConstructorDelegationCall
                val container = context.scope.ownerDescriptor
                assert(container is ConstructorDescriptor) { "Trying to resolve KtConstructorDelegationCall not in constructor. scope.ownerDescriptor = $container" }
                return resolveConstructorDelegationCall(
                    context,
                    delegationCall,
                    calleeExpression,
                    container.containingDeclaration as ClassDescriptor
                )
            }

            null -> {
                return checkArgumentTypesAndFail(context)
            }

            // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
            else -> {
                var expectedType: CangJieType = NO_EXPECTED_TYPE
                if (calleeExpression is CjLambdaExpression) {
                    val parameterNumber = calleeExpression.valueParameters.size
                    val parameterTypes: MutableList<CangJieType> = ArrayList(parameterNumber)
                    for (i in 0 until parameterNumber) {
                        parameterTypes.add(NO_EXPECTED_TYPE)
                    }
                    expectedType = createFunctionType(
                        builtIns, Annotations.EMPTY, null, emptyList(), parameterTypes, null, context.expectedType
                    )
                }
                val calleeType = expressionTypingServices.safeGetType(
                    context.scope,
                    calleeExpression,
                    expectedType,
                    context.dataFlowInfo,
                    context.inferenceSession,
                    context.trace
                )
                val expressionReceiver = create(calleeExpression, calleeType, context.trace.bindingContext)

                val call: Call = CallTransformer.CallForImplicitInvoke(
                    context.call.explicitReceiver, expressionReceiver, context.call,
                    false
                )
                val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, call, calleeType)
                return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
            }
        }
    }

    private fun resolveCallForInvoke(
        context: BasicCallResolutionContext,
        tracing: TracingStrategy
    ): OverloadResolutionResults<FunctionDescriptor> {
        return computeTasksAndResolveCall(
            context, OperatorNameConventions.INVOKE, tracing,
            NewResolutionOldInference.ResolutionKind.Invoke
        )
    }

    /**
     * 解析构造器委托调用
     *
     * 解析类构造器中的委托调用（this() 或 super()）。
     * 这些调用必须出现在构造器的第一条语句。
     *
     * 示例：
     * ```kotlin
     * class MyClass {
     *     init(x: Int) { /* ... */ }
     *     init(s: String) : this(s.toInt()) { /* ... */ }  // this() 委托
     * }
     *
     * class Child : Parent {
     *     init(x: Int) : super(x) { /* ... */ }  // super() 委托
     * }
     * ```
     *
     * @param trace 绑定追踪
     * @param scope 词法作用域
     * @param dataFlowInfo 数据流信息
     * @param constructorDescriptor 当前构造器描述符
     * @param call 构造器委托调用表达式
     * @param inferenceSession 类型推断会话（可选）
     * @return 重载解析结果，如果没有委托调用则返回 null
     */
    fun resolveConstructorDelegationCall(
        trace: BindingTrace, scope: LexicalScope, dataFlowInfo: DataFlowInfo,
        constructorDescriptor: ClassConstructorDescriptor,
        call: CjConstructorDelegationCall?,
        inferenceSession: InferenceSession?
    ): OverloadResolutionResults<ConstructorDescriptor>? {
        if (call == null) {
            return null
        }

        // Method returns `null` when there is nothing to resolve in trivial cases like `null` call expression or
        // when super call should be conventional enum constructor and super call should be empty
        val context = BasicCallResolutionContext.create(
            trace, scope,
            makeCall(null, null, call),
            NO_EXPECTED_TYPE,
            dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            false,
            languageVersionSettings,
            dataFlowValueFactory,
            inferenceSession ?: default
        )

        val calleeExpression = call.calleeExpression
            ?: return checkArgumentTypesAndFail(context)

        val currentClassDescriptor = constructorDescriptor.containingDeclaration

        if (constructorDescriptor.constructedClass.kind == ClassKind.ENUM && call.isImplicit) {
            if (currentClassDescriptor.unsubstitutedPrimaryConstructor != null) {
                // 默认启用严格检查：在枚举中必须有主构造函数委托调用
                val warningOrError = PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED // error
                val reportOn = calcReportOn(calleeExpression)
                context.trace.report(warningOrError.on(reportOn))
            }
            return null
        }

        return resolveConstructorDelegationCall(
            context, call,
            call.calleeExpression ?: return null, currentClassDescriptor
        )
    }

    private fun calcReportOn(calleeExpression: CjConstructorDelegationReferenceExpression): PsiElement {
        val delegationCall = calleeExpression.parent
        return delegationCall!!.reportOnElement()
    }

    private fun resolveCallForConstructor(
        context: BasicCallResolutionContext,
        expression: CjConstructorCalleeExpression
    ): OverloadResolutionResults<ConstructorDescriptor> {
        assert(context.call.explicitReceiver == null) { "Constructor can't be invoked with explicit receiver: " + context.call.callElement.text }

        context.trace.record(BindingContext.LEXICAL_SCOPE, context.call.callElement, context.scope)

        val functionReference: CjReferenceExpression? = expression.constructorReferenceExpression
        val typeReference = expression.typeReference
        if (functionReference == null || typeReference == null) {
            checkForConstructorCallOnFunctionalType(typeReference, context)
            return checkArgumentTypesAndFail(context) // No type there
        }
        val constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true)
        if (constructedType.isError) {
            return checkArgumentTypesAndFail(context)
        }

        val declarationDescriptor: DeclarationDescriptor? = constructedType.constructor.declarationDescriptor
        if (declarationDescriptor !is ClassDescriptor) {
            context.trace.report(NOT_A_CLASS.on(expression))
            return checkArgumentTypesAndFail(context)
        }

        val constructors = declarationDescriptor.constructors
        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(context.call.getValueArgumentListOrElement()))
            return checkArgumentTypesAndFail(context)
        }

        return resolveConstructorCall(context, functionReference, constructedType)
    }

    /**
     * 解析构造器调用
     *
     * 解析类型构造器的调用，如 `MyClass(arg1, arg2)`。
     * 处理构造器的重载解析、类型参数推断等。
     *
     * @param context 调用解析上下文
     * @param functionReference 构造器引用表达式
     * @param constructedType 被构造的类型
     * @return 重载解析结果
     */
    fun resolveConstructorCall(
        context: BasicCallResolutionContext,
        functionReference: CjReferenceExpression,
        constructedType: CangJieType
    ): OverloadResolutionResults<ConstructorDescriptor> {
        var context = context
        val candidatesAndContext =
            prepareCandidatesAndContextForConstructorCall(
                constructedType, context,
                syntheticScopes
            )

        val candidates = candidatesAndContext.first
        context = candidatesAndContext.second

        return computeTasksFromCandidatesAndResolvedCall(context, functionReference, candidates)
    }

    /**
     * 计算任务并解析调用
     *
     *
     * 该方法主要用于在给定的调用上下文中，计算所有可能的解析任务，并根据这些任务解析调用
     * 它结合了调用的基本上下文、调用的名字、追踪策略以及解析的种类，来执行具体的解析逻辑
     *
     * @param context 调用解析的基本上下文，包含了进行解析所需的所有信息
     * @param name    调用的名字，用于识别和区分不同的调用
     * @param tracing 追踪策略，用于在解析过程中追踪和记录解析的路径
     * @param kind    解析的种类，表示解析的具体类型，可以是函数、构造器等
     * @param <D>     CallableDescriptor的子类，表示可以被调用的描述符类型
     * @return 返回一个OverloadResolutionResults对象，包含了解析的结果和相关信息
    </D> */
    fun <D : CallableDescriptor> computeTasksAndResolveCall(
        context: BasicCallResolutionContext,
        name: Name,
        tracing: TracingStrategy,
        kind: NewResolutionOldInference.ResolutionKind
    ): OverloadResolutionResults<D> {
        // 通过性能计数器来记录解析调用的时间
        return callResolvePerfCounter.time {
            // 创建一个解析任务，该任务根据解析的种类、调用的名字等信息初始化
            val resolutionTask = ResolutionTask<D>(kind, name, null)
            doResolveCallOrGetCachedResults(context, resolutionTask, tracing)
        }
    }


    /**
     * 执行函数或方法的重载解析，或者获取缓存的解析结果
     * 该方法是实际进行重载解析的核心逻辑，它会根据不同的情况调用不同的解析策略
     *
     * @param context        调用解析的上下文信息，包括调用表达式、当前作用域等
     * @param resolutionTask 解析任务对象，包含了需要解析的描述符类型以及解析的种类
     * @param tracing        追踪策略对象，用于绑定调用并记录解析过程中的信息
     * @param <D>            可调用描述符的类型，继承自CallableDescriptor
     * @return 返回解析结果对象，包含成功或失败的解析信息
    </D> */
    private fun <D : CallableDescriptor> doResolveCallOrGetCachedResults(
        context: BasicCallResolutionContext,
        resolutionTask: ResolutionTask<D>,
        tracing: TracingStrategy
    ): OverloadResolutionResults<D> {
        // 获取调用表达式和追踪对象
        val call = context.call
        tracing.bindCall(context.trace, call)

        // 默认使用改进的类型推断系统
        val newInferenceEnabled = true
        val resolutionKind = resolutionTask.resolutionKind

        // 如果启用新推断功能且解析种类在默认解析种类列表中，则执行新的解析和推断过程
        if (newInferenceEnabled &&
            psiCallResolver.defaultResolutionKinds.contains(resolutionKind)
        ) {
            checkNotNull(resolutionTask.name)
            context.trace.recordScope(context.scope, context.call.calleeExpression)
            return psiCallResolver.runResolutionAndInference(context, resolutionTask.name, resolutionKind, tracing)
        }

        // 如果启用新推断功能且解析种类为给定候选，则执行针对给定候选的解析和推断过程
        if (newInferenceEnabled && resolutionKind is NewResolutionOldInference.ResolutionKind.GivenCandidates) {
            checkNotNull(resolutionTask.givenCandidates)
            context.trace.recordScope(context.scope, context.call.calleeExpression)
            return psiCallResolver.runResolutionAndInferenceForGivenCandidates(
                context,
                resolutionTask.givenCandidates,
                tracing
            )
        }

        // 创建一个临时的追踪对象，用于记录解析调用过程中的信息
        val traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", call)

        // 使用临时追踪对象替换原有的上下文对象
        val newContext = context.replaceBindingTrace(traceToResolveCall)

        // 执行重载解析，并返回解析结果
        return doResolveCall(newContext, resolutionTask, tracing)
    }


    private fun <D : CallableDescriptor> doResolveCall(
        context: BasicCallResolutionContext,
        resolutionTask: ResolutionTask<D>,
        tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<D> {
        context.dataFlowInfoForArguments.resultInfo
        if (context.checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) {
            argumentTypeResolver.analyzeArgumentsAndRecordTypes(
                context,
                ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS
            )
        }

        val typeArguments = context.call.typeArguments
        for (projection in typeArguments) {
            if (projection.projectionKind != CjProjectionKind.NONE) {
                context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
                check(
                    projection, context.trace, null,
                    languageVersionSettings
                )
            }
            val type = argumentTypeResolver.resolveTypeRefWithDefault(
                projection.typeReference, context.scope, context.trace,
                null
            )
            if (type != null) {
                forceResolveAllContents(type)
            }
        }

        val result: OverloadResolutionResultsImpl<D> =
            if (resolutionTask.resolutionKind !is NewResolutionOldInference.ResolutionKind.GivenCandidates) {

                newResolutionOldInference.runResolution(
                    context,
                    resolutionTask.name!!,
                    resolutionTask.resolutionKind,
                    tracing
                )
            } else {

                newResolutionOldInference.runResolutionForGivenCandidates(
                    context,
                    tracing,
                    resolutionTask.givenCandidates!!
                )
            }

        // in code like
        //   assert(a!!.isEmpty())
        //   a.length
        // we should ignore data flow info from assert argument, since assertions can be disabled and
        // thus it will lead to NPE in runtime otherwise
//        if (languageVersionSettings.getFlag(AnalysisFlags.getIgnoreDataFlowInAssert()) && result.isSingleResult()) {
//            D descriptor = result.getResultingDescriptor();
//            if (descriptor.getName().equals(Name.identifier("assert"))) {
//                DeclarationDescriptor declaration = descriptor.getContainingDeclaration();
//                if (declaration instanceof PackageFragmentDescriptor &&
//                        ((PackageFragmentDescriptor) declaration).getFqName().asString().equals("cangjie")) {
//                    context.dataFlowInfoForArguments.updateInfo(context.call.getValueArguments().get(0), initialInfo);
//                }
//            }
//        }
        return result
    }



    /**
     * 解析简单变量引用
     *
     * 解析对变量、属性或参数的简单引用。
     * 调用表达式必须是 [CjSimpleNameExpression]。
     *
     * 示例：
     * ```kotlin
     * val x = 10
     * println(x)  // 解析 x 的引用
     * ```
     *
     * @param context 调用解析上下文
     * @return 重载解析结果，包含变量描述符
     */
    fun resolveSimpleVariable(context: BasicCallResolutionContext): OverloadResolutionResults<VariableDescriptor> {
        val calleeExpression = context.call.calleeExpression
        assert(calleeExpression is CjSimpleNameExpression)
        val nameExpression = calleeExpression as CjSimpleNameExpression
        val referencedName = nameExpression.referencedNameAsName
        return computeTasksAndResolveCall(
            context, referencedName, nameExpression,
            NewResolutionOldInference.ResolutionKind.Variable
        )
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 解析任务
     *
     * 封装一次调用解析所需的任务信息。
     * 包含解析类型、名称和候选列表。
     *
     * @param D 可调用描述符类型
     * @property resolutionKind 解析类型（函数、变量、构造器等）
     * @property name 要解析的名称，可为 null（用于给定候选的情况）
     * @property givenCandidates 预先给定的候选列表，可为 null（用于按名称查找的情况）
     */
    private class ResolutionTask<D : CallableDescriptor>(
        val resolutionKind: NewResolutionOldInference.ResolutionKind,
        val name: Name?,  //                ,
        val givenCandidates: Collection<OldResolutionCandidate<D>>?
    )

    companion object {
        private val callResolvePerfCounter =
            create("Call resolve", ExpressionTypingVisitorDispatcher.typeInfoPerfCounter)

        private fun prepareCandidatesAndContextForConstructorCall(
            superType: CangJieType,
            context: BasicCallResolutionContext,
            syntheticScopes: SyntheticScopes
        ): Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext> {
            var context = context
            if (superType.constructor.declarationDescriptor !is ClassDescriptor) {
                return Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext>(
                    emptyList(), context
                )
            }

            // If any constructor has type parameter (currently it only can be true for ones from Java), try to infer arguments for them
            // Otherwise use NO_EXPECTED_TYPE and known type substitutor
            val anyConstructorHasDeclaredTypeParameters =
                anyConstructorHasDeclaredTypeParameters(superType.constructor.declarationDescriptor)

            if (anyConstructorHasDeclaredTypeParameters) {
                context = context.replaceExpectedType(superType)
            }

            val candidates =
                createResolutionCandidatesForConstructors(
                    context.scope, context.call, superType, !anyConstructorHasDeclaredTypeParameters, syntheticScopes
                )

            return Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext>(
                candidates,
                context
            )
        }

        private fun anyConstructorHasDeclaredTypeParameters(classDescriptor: ClassifierDescriptor?): Boolean {
            if (classDescriptor !is ClassDescriptor) return false
            for (constructor in classDescriptor.constructors) {
                if (constructor.typeParameters.size > constructor.containingDeclaration
                        .declaredTypeParameters.size
                ) return true
            }

            return false
        }
    }
}
