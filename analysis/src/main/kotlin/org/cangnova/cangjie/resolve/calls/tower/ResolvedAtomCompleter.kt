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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.CjCallableReference
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.DoubleColonExpressionResolver
import org.cangnova.cangjie.resolve.MissingSupertypesResolver
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.caches.MissingDependencySupertypeChecker
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import org.cangnova.cangjie.resolve.calls.checkers.CallCheckerContext
import org.cangnova.cangjie.resolve.calls.components.CallableReferenceAdaptation
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.isVararg
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.inference.ComposedSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.EmptySubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.AbstractTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.TypeSubstitutorByConstructorMap
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategyImpl
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.resolve.calls.util.extractCallableReferenceExpression
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import org.cangnova.cangjie.types.expressions.CoercionStrategy

import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo

/**
 * 已解析原子完成器
 *
 * 负责在类型推断完成后，完成已解析的原子（ResolvedAtom）。
 * 这个类处理调用、可调用引用等的最终解析，包括类型替换、参数适配、
 * 调用检查等后续处理。
 *
 * @property resultSubstitutor 结果类型替换器，用于应用类型推断的结果
 * @property topLevelCallContext 顶层调用解析上下文
 * @property cangjieToResolvedCallTransformer 仓颉调用到已解析调用的转换器
 * @property expressionTypingServices 表达式类型服务
 * @property argumentTypeResolver 参数类型解析器
 * @property doubleColonExpressionResolver 双冒号表达式解析器（可调用引用）
 * @property builtIns 内置类型
 * @property deprecationResolver 弃用解析器
 * @property moduleDescriptor 模块描述符
 * @property dataFlowValueFactory 数据流值工厂
 * @property typeApproximator 类型近似器
 * @property missingSupertypesResolver 缺失父类型解析器
 * @property callComponents 调用组件
 */
class ResolvedAtomCompleter(
    private val resultSubstitutor: ComposableTypeSubstitutor,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val expressionTypingServices: ExpressionTypingServices,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,

    private val builtIns: CangJieBuiltIns,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val callComponents: CangJieCallComponents,

    ) {
    // 顶层调用检查上下文
    private val topLevelCallCheckerContext = CallCheckerContext(
        topLevelCallContext, deprecationResolver, moduleDescriptor,
        missingSupertypesResolver,
        callComponents,
    )

    // 顶层绑定跟踪器
    private val topLevelTrace = topLevelCallCheckerContext.trace

    /**
     * 从部分解析的调用中提取诊断信息
     *
     * @param resolvedCallAtom 已解析的调用原子
     * @return 诊断信息集合
     */
    private fun extractDiagnosticsFromPartiallyResolvedCall(resolvedCallAtom: ResolvedCallAtom): Set<CangJieCallDiagnostic> {
        val psiCall = CangJieToResolvedCallTransformer.keyForPartiallyResolvedCall(resolvedCallAtom)
        val partialCallContainer = topLevelTrace[BindingContext.ONLY_RESOLVED_CALL, psiCall]

        return partialCallContainer?.result?.diagnostics.orEmpty().toSet()
    }

    /**
     * 完成所有已解析的原子
     *
     * 递归地完成已解析原子及其所有子原子。这是完成过程的入口点。
     *
     * @param resolvedAtom 待完成的已解析原子
     */
    fun completeAll(resolvedAtom: ResolvedAtom) {
        // 如果原子未分析，则跳过
        if (!resolvedAtom.analyzed)
            return
        // 递归完成所有子原子
        resolvedAtom.subResolvedAtoms?.forEach { subCjPrimitive ->
            completeAll(subCjPrimitive)
        }
        // 完成当前原子
        complete(resolvedAtom)
    }

    /**
     * 完成单个已解析原子
     *
     * 根据原子的类型（可调用引用、调用等），调用相应的完成方法。
     * 如果原子已经完成，则跳过。
     *
     * @param resolvedAtom 待完成的已解析原子
     */
    private fun complete(resolvedAtom: ResolvedAtom) {
        // 检查是否已经完成
        if (topLevelCallContext.inferenceSession.callCompleted(resolvedAtom)) {
            return
        }

        // 根据原子类型分发到不同的完成方法
        when (resolvedAtom) {
//                is ResolvedCollectionLiteralAtom -> completeCollectionLiteralCalls(resolvedAtom)
            is ResolvedCallableReferenceArgumentAtom -> completeCallableReferenceArgument(resolvedAtom)
//                is ResolvedLambdaAtom -> completeLambda(resolvedAtom)
            is ResolvedCallAtom -> completeResolvedCall(resolvedAtom, emptyList())
//                is ResolvedSubCallArgument -> completeSubCallArgument(resolvedAtom)
//                is ResolvedExpressionAtom -> completeExpression(resolvedAtom)
            else -> {}
        }
    }

    /**
     * 完成可调用引用参数
     *
     * 处理可调用引用（如 ::functionName）的完成，包括类型推断结果的应用、
     * 描述符绑定和类型信息记录。
     *
     * @param resolvedAtom 已解析的可调用引用参数原子
     * @return 可调用引用的结果类型，如果已完成或失败则返回 null
     */
    fun completeCallableReferenceArgument(resolvedAtom: ResolvedCallableReferenceArgumentAtom): CangJieType? {
        // 如果已完成，直接返回
        if (resolvedAtom.completed) return null

        val psiCallArgument = resolvedAtom.atom.psiCallArgument as CallableReferenceCangJieCallArgumentImpl
        val callableReferenceCallCandidate = resolvedAtom.candidate ?: return null
        // 从绑定上下文中获取已记录的描述符
        val descriptor = when (callableReferenceCallCandidate.candidate) {
            is FunctionDescriptor -> topLevelCallContext.trace.get(
                BindingContext.FUNCTION,
                psiCallArgument.cjCallableReferenceExpression
            )
//
//            is VariableCallableDescriptor -> topLevelCallContext.trace.get(
//                BindingContext.VARIABLE,
//                psiCallArgument.cjCallableReferenceExpression
//            )

            else -> null
        }
        val dataFlowInfo = resolvedAtom.atom.psiCallArgument.dataFlowInfoAfterThisArgument
        val resolvedCall = CallableReferenceResolvedCall<CallableDescriptor>(
            resolvedAtom,
            typeApproximator,
            expressionTypingServices.languageVersionSettings
        )

        return completeCallableReference(callableReferenceCallCandidate, descriptor, resolvedCall, dataFlowInfo)
            .also { resolvedAtom.completed = true }
    }

    /**
     * 可调用引用结果类型信息
     *
     * 封装了可调用引用完成后的类型信息，包括接收者和类型替换器。
     *
     * @property dispatchReceiver 分发接收者值（成员引用的目标对象）
     * @property explicitReceiver 显式接收者值（用户明确指定的接收者）
     * @property substitutor 类型替换器，用于应用类型参数
     * @property resultType 最终的引用类型（如函数类型）
     */
    private data class CallableReferenceResultTypeInfo(
        val dispatchReceiver: ReceiverValue?,
        val explicitReceiver: ReceiverValue?,
        val substitutor: ComposableTypeSubstitutor,
        val resultType: CangJieType
    )

    /**
     * 从描述符中提取可调用引用的结果类型信息
     *
     * 当描述符已经被记录时（如在早期解析中），从描述符中提取类型信息。
     *
     * @param callableCandidate 可调用引用候选项
     * @param recordedDescriptor 已记录的可调用描述符
     * @return 可调用引用的结果类型信息
     */
    private fun extractCallableReferenceResultTypeInfoFromDescriptor(
        callableCandidate: CallableReferenceResolutionCandidate,
        recordedDescriptor: CallableDescriptor
    ): CallableReferenceResultTypeInfo {
        // 确定分发接收者（优先使用记录的描述符，否则使用候选项中的）
        val dispatchReceiver = recordedDescriptor.dispatchReceiverParameter?.value
            ?: callableCandidate.dispatchReceiver?.receiver?.receiverValue
        // 根据显式接收者类型确定显式接收者值
        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiver
            else -> null
        }

        return CallableReferenceResultTypeInfo(
            dispatchReceiver,
            explicitCallableReceiver,
            ComposableTypeSubstitutor.EMPTY,
            // 用描述符的签名替换函数类型的参数类型
            callableCandidate.reflectionCandidateType.replaceFunctionTypeArgumentsByDescriptor(recordedDescriptor)
        )
    }

    /**
     * 使用描述符的签名替换函数类型的参数类型
     *
     * 将可调用引用的反射类型（如 (T) -> R）中的类型参数替换为
     * 实际描述符的参数和返回类型。
     *
     * @param descriptor 可调用描述符
     * @return 替换后的函数类型
     */
    private fun CangJieType.replaceFunctionTypeArgumentsByDescriptor(descriptor: CallableDescriptor) =
        when (descriptor) {
            is CallableMemberDescriptor -> {
                // 构建新的参数类型列表：所有值参数类型 + 返回类型
                val newArgumentTypes = buildList {
                    addAll(descriptor.valueParameters.map { it.type })
                    add(descriptor.returnType)
                }
                // 如果数量匹配，替换类型参数
                if (newArgumentTypes.size == arguments.size) {
                    replace(arguments.mapIndexed { i, type ->
                        newArgumentTypes[i]?.let { type.replaceType(it) } ?: type
                    })
                } else this
            }

            is ValueDescriptor -> replace(descriptor.type.arguments)
            else -> this
        }

    /**
     * 为可调用引用记录参数适配信息
     *
     * 当可调用引用需要参数适配时（如默认参数、vararg 等），
     * 记录适配后的参数映射关系。
     *
     * @param resolvedCall 已解析的调用
     * @param callableReferenceAdaptation 可调用引用的参数适配信息
     */
    private fun recordArgumentAdaptationForCallableReference(
        resolvedCall: AbstractResolvedCall<*>,
        callableReferenceAdaptation: CallableReferenceAdaptation?
    ) {
        if (callableReferenceAdaptation == null) return

        val callElement = resolvedCall.call.callElement
        val isUnboundReference = resolvedCall.dispatchReceiver is TransientReceiver

        /**
         * 为可调用引用创建伪值参数
         *
         * @param callArgument 调用参数
         * @return 伪值参数
         */
        fun makeFakeValueArgument(callArgument: CangJieCallArgument): ValueArgument {
            val fakeCallArgument = callArgument as? FakeCangJieCallArgumentForCallableReference
                ?: throw AssertionError("FakeCangJieCallArgumentForCallableReference expected: $callArgument")
            return FakePositionalValueArgumentForCallableReferenceImpl(
                callElement,
                // 无界引用需要偏移索引（因为第一个参数是接收者）
                if (isUnboundReference) fakeCallArgument.index + 1 else fakeCallArgument.index
            )
        }

        // 只有在需要参数适配时才记录映射：
        // - 参数映射非平凡（有默认参数或 vararg）
        // - 需要结果强制转换
        var hasNonTrivialMapping = false
        val mappedArguments = ArrayList<Pair<ValueParameterDescriptor, ResolvedValueArgument>>()
        for ((valueParameter, resolvedCallArgument) in callableReferenceAdaptation.mappedArguments) {
            val resolvedValueArgument = when (resolvedCallArgument) {
                // 默认参数：使用默认值
                ResolvedCallArgument.DefaultArgument -> {
                    hasNonTrivialMapping = true
                    DefaultValueArgument.DEFAULT
                }

                // 简单参数：直接映射
                is ResolvedCallArgument.SimpleArgument -> {
                    val valueArgument = makeFakeValueArgument(resolvedCallArgument.callArgument)
                    // vararg 参数需要特殊处理
                    if (valueParameter.isVararg)
                        VarargValueArgument(
                            listOf(
                                FakeImplicitSpreadValueArgumentForCallableReferenceImpl(callElement, valueArgument)
                            )
                        )
                    else
                        ExpressionValueArgument(valueArgument)
                }

                // Vararg 参数：映射多个参数
                is ResolvedCallArgument.VarargArgument -> {
                    hasNonTrivialMapping = true
                    VarargValueArgument(
                        resolvedCallArgument.arguments.map {
                            makeFakeValueArgument(it)
                        }
                    )
                }
            }
            mappedArguments.add(valueParameter to resolvedValueArgument)
        }
        // 如果有非平凡映射或需要隐式转换，更新值参数映射
        if (hasNonTrivialMapping || isCallableReferenceWithImplicitConversion(
                resolvedCall,
                callableReferenceAdaptation
            )
        ) {
            resolvedCall.updateValueArguments(mappedArguments.toMap())
        }
    }

    /**
     * 判断可调用引用是否需要隐式转换
     *
     * @param resolvedCall 已解析的调用
     * @param callableReferenceAdaptation 可调用引用的参数适配信息
     * @return 如果需要隐式转换返回 true
     */
    private fun isCallableReferenceWithImplicitConversion(
        resolvedCall: AbstractResolvedCall<*>,
        callableReferenceAdaptation: CallableReferenceAdaptation
    ): Boolean {
        val resultingDescriptor = resolvedCall.resultingDescriptor

        // TODO 删除返回类型检查 - 参见 noCoercionToUnitIfFunctionAlreadyReturnsUnit.kt
        if (callableReferenceAdaptation.coercionStrategy == CoercionStrategy.COERCION_TO_UNIT && !resultingDescriptor.returnType!!.isUnit())
            return true

//        if (callableReferenceAdaptation.suspendConversionStrategy == SuspendConversionStrategy.SUSPEND_CONVERSION)
//            return true

        return false
    }

    /**
     * 完成可调用引用
     *
     * 执行可调用引用的完整完成过程，包括类型信息更新、参数适配记录、
     * 绑定跟踪和调用检查。
     *
     * @param callableCandidate 可调用引用候选项
     * @param recordedDescriptor 已记录的可调用描述符（可能为 null）
     * @param resolvedCall 已解析的调用
     * @param additionalDataFlowInfo 额外的数据流信息（可选）
     * @return 可调用引用的结果类型，失败则返回 null
     */
    private fun completeCallableReference(
        callableCandidate: CallableReferenceResolutionCandidate,
        recordedDescriptor: CallableDescriptor?,
        resolvedCall: AbstractResolvedCall<*>,
        additionalDataFlowInfo: DataFlowInfo? = null,
    ): CangJieType? {
        val callableReferenceExpression =
            callableCandidate.resolvedCall.atom.psiCangJieCall.extractCallableReferenceExpression() ?: return null


        // 获取结果类型信息（从描述符或更新引用类型）
        val resultTypeInfo = if (recordedDescriptor != null) {
            extractCallableReferenceResultTypeInfoFromDescriptor(callableCandidate, recordedDescriptor)
        } else {
            updateCallableReferenceResultType(callableCandidate)
        }

        if (resultTypeInfo == null) return null

        // 更新已解析调用的接收者和替换器
        resolvedCall.apply {
            if (resultTypeInfo.dispatchReceiver != null) {
                updateDispatchReceiverType(resultTypeInfo.dispatchReceiver.type)
            }

            setResultingSubstitutor(resultTypeInfo.substitutor)
        }

        // 记录参数适配信息
        recordArgumentAdaptationForCallableReference(resolvedCall, callableCandidate.callableReferenceAdaptation)

        // 创建 PSI 调用并进行绑定
        val psiCall = CallMaker.makeCall(
            callableReferenceExpression.callableReference,
            resultTypeInfo.explicitReceiver,
            null,
            callableReferenceExpression.callableReference,
            emptyList()
        )
        val tracing = TracingStrategyImpl.create(callableReferenceExpression.callableReference, psiCall)

        tracing.bindCall(topLevelTrace, psiCall)
        tracing.bindReference(topLevelTrace, resolvedCall)
        tracing.bindResolvedCall(topLevelTrace, resolvedCall)

        // TODO: 可能还需要记录 'DATA_FLOW_INFO_BEFORE' 键，参见 ExpressionTypingVisitorDispatcher.getTypeInfo
        val typeInfo = if (additionalDataFlowInfo != null) {
            createTypeInfo(resultTypeInfo.resultType, additionalDataFlowInfo)
        } else {
            createTypeInfo(resultTypeInfo.resultType)
        }

        // 记录表达式类型信息
        topLevelTrace.record(BindingContext.EXPRESSION_TYPE_INFO, callableReferenceExpression, typeInfo)
        topLevelTrace.record(BindingContext.PROCESSED, callableReferenceExpression)

        // 运行调用检查器
        cangjieToResolvedCallTransformer.runCallCheckers(resolvedCall, topLevelCallCheckerContext)

        return resultTypeInfo.resultType
    }

    /**
     * 判断可调用描述符是否支持可调用引用
     *
     * @return 如果是属性或函数则返回 true
     */
    fun CallableDescriptor.isSupportedForCallableReference() = this is PropertyDescriptor || this is FunctionDescriptor

    /**
     * 更新接收者值的类型
     *
     * 应用类型替换并进行类型近似，确保接收者类型正确。
     *
     * @param substitutor 类型替换器
     * @return 更新后的接收者值
     */
    private fun ReceiverValue.updateReceiverValue(substitutor: ComposableTypeSubstitutor): ReceiverValue {
        val newType = substitutor.safeSubstitute(type.unwrap()).let {
            // 近似到超类型以确保类型安全
            typeApproximator.approximateToSuperType(
                it,
                TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
            ) ?: it
        }
        return if (type != newType) replaceType(newType as CangJieType) else this
    }

    /**
     * 更新可调用引用的结果类型
     *
     * 当描述符未被记录时，从候选项中更新可调用引用的类型信息。
     * 这包括应用类型推断结果、更新接收者和绑定引用。
     *
     * @param callableCandidate 可调用引用候选项
     * @return 可调用引用的结果类型信息，失败则返回 null
     */
    private fun updateCallableReferenceResultType(callableCandidate: CallableReferenceResolutionCandidate): CallableReferenceResultTypeInfo? {
        val callableReferenceExpression =
            callableCandidate.resolvedCall.atom.psiCangJieCall.psiCall.callElement as? CjCallableReference
                ?: return null
        val freshSubstitutor = callableCandidate.freshVariablesSubstitutor ?: return null
        // 应用类型推断结果到类型参数
        val resultTypeParameters =
            freshSubstitutor.freshVariables.map { resultSubstitutor.safeSubstitute(it.defaultType) }

        val resultSubstitutor = if (callableCandidate.candidate.isSupportedForCallableReference()) {
            ComposableTypeSubstitutor.create(callableCandidate.candidate.typeParameters.map { it.typeConstructor }
                .zip(resultTypeParameters).toMap())
                .compose(resultSubstitutor)

        } else ComposableTypeSubstitutor.EMPTY

        // 写入可调用引用表达式的类型
        val resultType = resultSubstitutor.safeSubstitute(callableCandidate.reflectionCandidateType)

        // 如果类型不可表示，更新结果参数类型
        argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
            topLevelTrace, expressionTypingServices.statementFilter, resultType, callableReferenceExpression
        )

        val dispatchReceiver =
            callableCandidate.dispatchReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)

        // 根据候选项类型绑定引用
        when (callableCandidate.candidate) {
            is FunctionDescriptor -> doubleColonExpressionResolver.bindFunctionReference(
                callableReferenceExpression,
                resultType,
                topLevelCallContext,
                callableCandidate.candidate
            )

//            is PropertyDescriptor -> doubleColonExpressionResolver.bindPropertyReference(
//                callableReferenceExpression,
//                resultType,
//                topLevelCallContext
//            )
        }


        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> callableCandidate.dispatchReceiver
            else -> null
        }
        val explicitReceiver = explicitCallableReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)

        return CallableReferenceResultTypeInfo(
            dispatchReceiver,
            explicitReceiver,
            resultSubstitutor,
            resultType
        )
    }

    /**
     * 检查引用是否指向允许的成员
     *
     * 验证可调用引用是否指向合法的成员（如非私有成员）。
     * 当前实现为空，可能用于未来扩展。
     *
     * @param descriptor 可调用描述符
     * @param trace 绑定跟踪器
     * @param expression 可调用引用表达式
     */
    internal fun checkReferenceIsToAllowedMember(
        descriptor: CallableDescriptor, trace: BindingTrace, expression: CjCallableReference
    ) {

    }

    /**
     * 完成已解析的调用
     *
     * 处理普通调用（非可调用引用）的完成过程，包括类型转换、
     * 绑定记录、参数检查和调用检查。
     *
     * @param resolvedCallAtom 已解析的调用原子
     * @param diagnostics 诊断信息集合
     * @return 完成后的已解析调用，失败则返回 null
     */
    fun completeResolvedCall(
        resolvedCallAtom: ResolvedCallAtom,
        diagnostics: Collection<CangJieCallDiagnostic>
    ): AbstractResolvedCall<*>? {
        // 从部分解析的调用中提取额外的诊断信息
        val diagnosticsFromPartiallyResolvedCall = extractDiagnosticsFromPartiallyResolvedCall(resolvedCallAtom)

//        clearPartiallyResolvedCall(resolvedCallAtom)

        val atom = resolvedCallAtom.atom
        // 变量作为调用（如 invoke）不需要完成
        if (atom.psiCangJieCall is PSICangJieCallForVariable) return null

        val allDiagnostics = diagnostics + diagnosticsFromPartiallyResolvedCall

        // 将调用原子转换为已解析调用
        val resolvedCall = cangjieToResolvedCallTransformer.transformToResolvedCall<CallableDescriptor>(
            resolvedCallAtom,
            topLevelTrace,
            resultSubstitutor,
            allDiagnostics
        )

        // 获取最终的调用（处理 VariableAsFunctionResolvedCall 包装）
        val lastCall = if (resolvedCall is VariableAsFunctionResolvedCall) {
            resolvedCall.functionCall as AbstractResolvedCall<*>
        } else resolvedCall
        // 如果候选描述符是错误描述符，只运行参数检查
        if (ErrorUtils.isError(resolvedCall.candidateDescriptor)) {
            cangjieToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall)
            checkMissingReceiverSupertypes(resolvedCall, missingSupertypesResolver, topLevelTrace)
            return resolvedCall
        }
//
        // 确定用于解析上下文的 PSI 调用（invoke 调用使用基础调用）
        val psiCallForResolutionContext = when (atom) {
            // PARTIAL_CALL_RESOLUTION_CONTEXT 已为 baseCall 写入
            is PSICangJieCallForInvoke -> atom.baseCall.psiCall
            else -> atom.psiCangJieCall.psiCall
        }
//
        val callElement = psiCallForResolutionContext.callElement
        // 如果调用元素是表达式且类型需要更新，记录新类型
        if (callElement is CjExpression) {
            val recordedType = topLevelCallContext.trace.getType(callElement)
            if (recordedType != null && recordedType.shouldBeUpdated() && resolvedCall.resultingDescriptor.returnType != null) {
                topLevelCallContext.trace.recordType(callElement, resolvedCall.resultingDescriptor.returnType)
            }
        }


        // 获取或创建调用检查器上下文
        val resolutionContextForPartialCall =
            topLevelCallContext.trace[BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, psiCallForResolutionContext]

        val callCheckerContext = if (resolutionContextForPartialCall != null)
            CallCheckerContext(
                resolutionContextForPartialCall.replaceBindingTrace(topLevelTrace),
                deprecationResolver,
                moduleDescriptor,
                missingSupertypesResolver,
                callComponents,
            )
        else
            topLevelCallCheckerContext

        // 绑定已解析调用
        cangjieToResolvedCallTransformer.bind(topLevelTrace, resolvedCall)
//
        // 运行各种检查
        cangjieToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall)
        cangjieToResolvedCallTransformer.runCallCheckers(resolvedCall, callCheckerContext)
//        cangjieToResolvedCallTransformer.runAdditionalReceiversCheckers(resolvedCall, topLevelCallContext)
//
        // 报告诊断信息
        cangjieToResolvedCallTransformer.reportDiagnostics(
            topLevelCallContext,
            topLevelTrace,
            resolvedCall,
            allDiagnostics
        )

        return resolvedCall
    }

    /**
     * 检查接收者的缺失父类型
     *
     * 验证接收者类型的父类型是否存在（用于检测缺失的依赖）。
     *
     * @param resolvedCall 已解析的调用
     * @param missingSupertypesResolver 缺失父类型解析器
     * @param trace 绑定跟踪器
     */
    private fun checkMissingReceiverSupertypes(
        resolvedCall: ResolvedCall<CallableDescriptor>,
        missingSupertypesResolver: MissingSupertypesResolver,
        trace: BindingTrace
    ) {
        val receiverValue = resolvedCall.dispatchReceiver
        receiverValue?.type?.let { receiverType ->
            MissingDependencySupertypeChecker.checkSupertypes(
                receiverType,
                resolvedCall.call.callElement,
                trace,
                missingSupertypesResolver
            )
        }
    }
}
