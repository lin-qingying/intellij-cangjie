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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.extensions.internal.CandidateInterceptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.CALL
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.IS_FUNC
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.LEXICAL_SCOPE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.NEW_INFERENCE_TRY_EXCEPTION_PARAMETER
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.ONLY_RESOLVED_CALL
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.QUALIFIER
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.TemporaryBindingTrace
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import org.cangnova.cangjie.resolve.calls.CallTransformer
import org.cangnova.cangjie.resolve.calls.CangJieCallResolver
import org.cangnova.cangjie.resolve.calls.SPECIAL_FUNCTION_NAMES
import org.cangnova.cangjie.resolve.calls.checkers.CallCheckerWithAdditionalResolve
import org.cangnova.cangjie.resolve.calls.checkers.PassingProgressionAsCollectionCallChecker
import org.cangnova.cangjie.resolve.calls.checkers.ResolutionWithStubTypesChecker
import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzer
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.inference.buildResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import org.cangnova.cangjie.resolve.calls.inference.components.ResultTypeResolver
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.tasks.DynamicCallableDescriptors
import org.cangnova.cangjie.resolve.calls.tasks.OldResolutionCandidate
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.util.*
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.DeferredType
import org.cangnova.cangjie.types.TypeApproximator
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext

import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.types.isFunctionType
import org.cangnova.cangjie.types.model.TypeSystemInferenceExtensionContext
import org.cangnova.cangjie.utils.compactIfPossible
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl
import org.cangnova.cangjie.utils.firstIsInstanceOrNull
import org.cangnova.cangjie.utils.isUnderscoreNamed

class PSICallResolver(
    private val typeResolver: TypeResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val qualifiedExpressionResolver: QualifiedExpressionResolver,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,

    private val languageVersionSettings: LanguageVersionSettings,
    private val dynamicCallableDescriptors: DynamicCallableDescriptors,
    private val syntheticScopes: SyntheticScopes,
    private val callComponents: CangJieCallComponents,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val cangjieCallResolver: CangJieCallResolver,
    private val typeApproximator: TypeApproximator,
    private val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter,
    private val argumentTypeResolver: ArgumentTypeResolver,
//    private val effectSystem: EffectSystem,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val candidateInterceptor: CandidateInterceptor,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val resultTypeResolver: ResultTypeResolver,
) {
    private val callCheckersWithAdditionalResolve = listOf<CallCheckerWithAdditionalResolve>(
        PassingProgressionAsCollectionCallChecker(cangjieCallResolver),
        ResolutionWithStubTypesChecker(cangjieCallResolver)
    )
    val defaultResolutionKinds = setOf(
        NewResolutionOldInference.ResolutionKind.Function,
        NewResolutionOldInference.ResolutionKind.Variable,
        NewResolutionOldInference.ResolutionKind.Invoke,
//        NewResolutionOldInference.ResolutionKind.Enum,
        NewResolutionOldInference.ResolutionKind.EnumEntry,

        NewResolutionOldInference.ResolutionKind.CaseEnum,
        NewResolutionOldInference.ResolutionKind.CallableReference
    )

    fun createCallableReferenceCangJieCallArgument(
        context: BasicCallResolutionContext,
        cjExpression: CjCallableReference,
        startDataFlowInfo: DataFlowInfo,
        valueArgument: ValueArgument,
        argumentName: Name?,
        outerCallContext: BasicCallResolutionContext,
        tracingStrategy: TracingStrategy
    ): CallableReferenceCangJieCallArgumentImpl {

        checkNoSpread(outerCallContext, valueArgument)

        val lhsResult = getLhsResult(context, cjExpression)
        val newDataFlowInfo =/* (doubleColonLhs as? DoubleColonLHS.Expression)?.dataFlowInfo ?: */startDataFlowInfo
        val rhsExpression = cjExpression.callableReference
        val rhsName = rhsExpression.referencedNameAsName
        val call = outerCallContext.trace[CALL, rhsExpression]
            ?: CallMaker.makeCall(
                rhsExpression,
                if (lhsResult is LHSResult.Type) lhsResult.qualifier else null,
                null,
                rhsExpression,
                emptyList()
            )
        val cangjieCall = toCangJieCall(
            context,
            CangJieCallKind.CALLABLE_REFERENCE,
            call,
            rhsName,
            tracingStrategy,
            isSpecialFunction = false
        )

        return CallableReferenceCangJieCallArgumentImpl(
            ASTScopeTower(context, rhsExpression), valueArgument, startDataFlowInfo,
            newDataFlowInfo, cjExpression, argumentName, lhsResult, rhsName, cangjieCall
        )
    }

    inner class FactoryProviderForInvoke(
        val context: BasicCallResolutionContext,
        val scopeTower: ImplicitScopeTower,
        val cangjieCall: PSICangJieCallImpl
    ) : CandidateFactoryProviderForInvoke<ResolutionCandidate> {

        init {
            assert(cangjieCall.dispatchReceiverForInvokeExtension == null) { cangjieCall }
        }

        override fun transformCandidate(
            variable: ResolutionCandidate,
            invoke: ResolutionCandidate
        ) = invoke

        override fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<ResolutionCandidate> {
            val explicitReceiver = if (stripExplicitReceiver) null else cangjieCall.explicitReceiver
            val variableCall = PSICangJieCallForVariable(cangjieCall, explicitReceiver, cangjieCall.name)
            return SimpleCandidateFactory(callComponents, scopeTower, variableCall, createResolutionCallbacks(context))
        }

        override fun factoryForInvoke(variable: ResolutionCandidate, useExplicitReceiver: Boolean):
                Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<ResolutionCandidate>>? {
            if (isRecursiveVariableResolution(variable)) return null

            assert(variable.isSuccessful) {
                "Variable call should be successful: $variable " +
                        "Descriptor: ${variable.resolvedCall.candidateDescriptor}"
            }
            val variableCallArgument = createReceiverCallArgument(variable)

            val explicitReceiver = cangjieCall.explicitReceiver
            val callForInvoke = if (useExplicitReceiver && explicitReceiver != null) {
                PSICangJieCallForInvoke(cangjieCall, variable, explicitReceiver, variableCallArgument)
            } else {
                PSICangJieCallForInvoke(cangjieCall, variable, variableCallArgument, null)
            }

            return variableCallArgument.receiver to SimpleCandidateFactory(
                callComponents, scopeTower, callForInvoke, createResolutionCallbacks(context)
            )
        }

        // todo: create special check that there is no invoke on variable
        private fun isRecursiveVariableResolution(variable: ResolutionCandidate): Boolean {
            val variableType = variable.resolvedCall.candidateDescriptor.returnType
            return variableType is DeferredType && variableType.isComputing
        }

        // todo: review
        private fun createReceiverCallArgument(variable: ResolutionCandidate): SimpleCangJieCallArgument {
            variable.forceResolution()
            val variableReceiver = createReceiverValueWithSmartCastInfo(variable)
            if (variableReceiver.hasTypesFromSmartCasts()) {
                return ReceiverExpressionCangJieCallArgument(
                    createReceiverValueWithSmartCastInfo(variable),
                    isForImplicitInvoke = true
                )
            }

            val psiCangJieCall = variable.resolvedCall.atom.psiCangJieCall

            val variableResult = PartialCallResolutionResult(variable.resolvedCall, listOf(), variable.getSystem())

            return SubCangJieCallArgumentImpl(
                CallMaker.makeExternalValueArgument((variableReceiver.receiverValue as ExpressionReceiver).expression),
                psiCangJieCall.resultDataFlowInfo, psiCangJieCall.resultDataFlowInfo, variableReceiver,
                variableResult
            )
        }

        // todo: decrease hacks count
        private fun createReceiverValueWithSmartCastInfo(variable: ResolutionCandidate): ReceiverValueWithSmartCastInfo {
            val callForVariable = variable.resolvedCall.atom as PSICangJieCallForVariable
            val calleeExpression = callForVariable.baseCall.psiCall.calleeExpression as? CjReferenceExpression
                ?: error("Unexpected call : ${callForVariable.baseCall.psiCall}")

            val temporaryTrace = TemporaryBindingTrace.create(context.trace, "Context for resolve candidate")

            val type = variable.resolvedCall.freshReturnType!!
            val variableReceiver = ExpressionReceiver.create(calleeExpression, type, temporaryTrace.bindingContext)

            temporaryTrace.record(
                REFERENCE_TARGET,
                calleeExpression,
                variable.resolvedCall.candidateDescriptor as DeclarationDescriptor
            )
            val dataFlowValue =
                dataFlowValueFactory.createDataFlowValue(
                    variableReceiver,
                    temporaryTrace.bindingContext,
                    context.scope.ownerDescriptor
                )
            return ReceiverValueWithSmartCastInfo(
                variableReceiver,
                context.dataFlowInfo.getCollectedTypes(dataFlowValue, context.languageVersionSettings)
                    .compactIfPossible(),
                dataFlowValue.isStable
            ).prepareReceiverRegardingCaptureTypes()
        }
    }


    /**
     * 将给定的调用解析结果转换为过载解析结果.
     *
     * 此函数负责根据基本调用解析上下文和 tracing 策略，将调用解析结果转换为过载解析结果.
     * 它处理各种类型的调用解析结果，并根据情况返回相应的过载解析结果.
     *
     * @param context 基本调用解析上下文，包含调用信息和配置.
     * @param result 调用解析结果，可能包含多个候选者或错误信息.
     * @param tracingStrategy 追踪策略，用于在解析过程中追踪和报告.
     * @param <D> 可调用描述符的类型，继承自 CallableDescriptor.
     * @return 返回转换后的过载解析结果.
     */
    fun <D : CallableDescriptor> convertToOverloadResolutionResults(
        context: BasicCallResolutionContext,
        result: CallResolutionResult,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        // 如果结果是所有候选者的解析结果，则将每个候选者转换为 resolved call，并返回所有候选者的过载解析结果.
        if (result is AllCandidatesResolutionResult) {
            val resolvedCalls = result.allCandidates.map { (candidate, diagnostics) ->
                val system = candidate.getSystem()
                val resultingSubstitutor =
                    system.asReadOnlyStorage().buildResultingSubstitutor(system as TypeSystemInferenceExtensionContext)

                cangjieToResolvedCallTransformer.transformToResolvedCall<D>(
                    candidate.resolvedCall, null, resultingSubstitutor, diagnostics
                )
            }

            return AllCandidates(resolvedCalls)
        }

        // 处理错误解析结果，如果结果是错误，则记录错误信息并返回错误结果.
        val trace = context.trace

        handleErrorResolutionResult<D>(context, trace, result, tracingStrategy)?.let { errorResult ->
            context.inferenceSession.addErrorCallInfo(PSIErrorCallInfo(result, errorResult))
            return errorResult
        }

        // 将解析结果转换为 resolved call，并记录转换过程中的诊断信息.
        val resolvedCall = cangjieToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

        // 注意：移动此调用的位置可能会导致副作用，因为效果系统期望解析结果被写入 trace 中.
        // resolvedCall.recordEffects(trace)

        // 返回单个 resolved call 的过载解析结果.
        return SingleOverloadResolutionResult(resolvedCall)
    }


    private val givenCandidatesName = Name.special("<given candidates>")

    /**
     * 为给定的候选函数运行解析和推断过程。
     *
     * 该函数负责根据提供的解析候选对象来执行过载解析和类型推断。它包括准备数据、调用具体解析方法和转换结果。
     *
     * @param context 基本调用解析上下文，包含解析所需的基本信息和配置。
     * @param resolutionCandidates 解析候选集合，每个候选代表一个可能的函数匹配。
     * @param tracingStrategy 跟踪策略，定义了如何记录解析过程中的决策和信息。
     * @param <D> 可调用描述符类型，限定为CallableDescriptor的子类型，用于描述函数或其他可调用成员。
     * @return 返回过载解析结果，包含解析后的函数及其相关推断信息。
     */
    fun <D : CallableDescriptor> runResolutionAndInferenceForGivenCandidates(
        context: BasicCallResolutionContext,
        resolutionCandidates: Collection<OldResolutionCandidate<D>>,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        // 获取第一个非空的dispatch接收者，用于后续的特殊函数判断和调用。
        val dispatchReceiver = resolutionCandidates.firstNotNullOfOrNull { it.dispatchReceiver }

        // 判断是否有特殊函数，特殊函数需要特别的处理逻辑。
        val isSpecialFunction = resolutionCandidates.any { it.descriptor.name in SPECIAL_FUNCTION_NAMES }
        // 将当前调用转换为仓颉调用模型，以便进行统一的解析处理。
        val cangjieCall = toCangJieCall(
            context,
            CangJieCallKind.FUNCTION,
            context.call,
            givenCandidatesName,
            tracingStrategy,
            isSpecialFunction,
            dispatchReceiver
        )
        // 构建AST作用域塔，用于解析过程中作用域的管理。
        val scopeTower = ASTScopeTower(context)
        // 创建解析回调函数列表，用于在解析过程中进行各种操作和处理。
        val resolutionCallbacks = createResolutionCallbacks(context)

        // 将解析候选对象转换为GivenCandidate形式，这是解析过程需要的数据结构。
        val givenCandidates = resolutionCandidates.map {
            GivenCandidate(
                it.descriptor as FunctionDescriptor,
                it.dispatchReceiver?.let { context.transformToReceiverWithSmartCastInfo(it) },
                it.knownTypeParametersResultingSubstitutor
            )
        }

        // 调用仓颉解析器来对给定的候选函数进行解析和完成，这是解析过程的核心部分。
        val result = cangjieCallResolver.resolveAndCompleteGivenCandidates(
            scopeTower,
            resolutionCallbacks,
            cangjieCall,
            calculateExpectedType(context),
            givenCandidates,
            context.collectAllCandidates
        )
        // 将解析结果转换为过载解析结果形式，以便返回和使用。
        val overloadResolutionResults = convertToOverloadResolutionResults<D>(context, result, tracingStrategy)
        return overloadResolutionResults.also {
            // 解析完成后清除近似结果的缓存，以避免潜在的内存泄漏和保持一致性。
            clearCacheForApproximationResults()
        }
    }

    /**
     * 处理错误的重载解析结果
     *
     * 此函数旨在处理各种重载解析错误情况，根据不同的诊断结果执行相应的处理逻辑
     * 它通过分析[CallResolutionResult]中的诊断信息，来确定应如何记录和处理这些错误
     *
     * @param context 基本调用解析上下文，包含解析所需的上下文信息
     * @param trace 绑定跟踪对象，用于记录解析过程中的关键信息
     * @param result 调用解析结果，包含诊断信息和可能的解析候选
     * @param tracingStrategy 跟踪策略，定义了如何记录解析过程中的事件
     * @return 返回处理后的重载解析结果，如果没有适用的结果则返回null
     */
    private fun <D : CallableDescriptor> handleErrorResolutionResult(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        result: CallResolutionResult,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D>? {
        // 获取解析结果中的诊断信息
        val diagnostics = result.diagnostics

        // 如果诊断信息表明没有合适的候选调用
        diagnostics.firstIsInstanceOrNull<NoneCandidatesCallDiagnostic>()?.let {
            // 将结果转换为ResolvedCall并报告诊断信息
            cangjieToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

            // 记录未解决的引用错误
            tracingStrategy.unresolvedReference(trace)
            // 返回名称未找到的结果
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        // 如果诊断信息表明有多个候选调用
        diagnostics.firstIsInstanceOrNull<ManyCandidatesCallDiagnostic>()?.let {
            // 将结果转换为ResolvedCall并报告诊断信息
            cangjieToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

            // 转换多个候选并记录跟踪信息
            return transformManyCandidatesAndRecordTrace(it, tracingStrategy, trace, context)
        }

        // 如果诊断信息表明候选不适用，且原因是因为接收者不正确
        if (getResultApplicability(diagnostics.filterErrorDiagnostics()) == CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER) {
            // 获取唯一的候选调用，这里假设结果不应该为空
            val singleCandidate = result.resultCallAtom() ?: error("Should be not null for result: $result")
            // 将唯一的候选转换为ResolvedCall，并记录错误接收者的跟踪信息
            val resolvedCall = cangjieToResolvedCallTransformer.onlyTransform<D>(singleCandidate, diagnostics).also {
                tracingStrategy.unresolvedReferenceWrongReceiver(trace, listOf(it))
            }

            // 返回包含单个解析调用的结果
            return SingleOverloadResolutionResult(resolvedCall)
        }

        // 如果没有匹配上述任何条件，则返回null
        return null
    }

    private fun Collection<ResolutionCandidate>.areAllFailed() =
        all {
            !it.resultingApplicability.isSuccess
        }

    private fun Collection<ResolutionCandidate>.areAllFailedWithInapplicableWrongReceiver() =
        all {
            it.resultingApplicability == CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
        }

    /**
     * 处理多个候选调用的情况并记录trace
     *
     * 该函数主要用于处理含有多个候选调用的诊断情况它会根据不同的条件记录相应的诊断信息，
     * 包括但不限于记录歧义、不可解析的引用、错误的接收者等信息同时，它还会根据上下文和解析结果
     * 的不同，选择合适的诊断策略进行记录
     *
     * @param diagnostic 包含多个候选调用的诊断信息
     * @param tracingStrategy 用于记录诊断信息的策略
     * @param trace 绑定跟踪对象，用于记录解析过程中的信息
     * @param context 基本调用解析上下文，包含解析所需的上下文信息
     * @return 返回一个包含已解析调用的 ManyCandidates 对象
     */
    private fun <D : CallableDescriptor> transformManyCandidatesAndRecordTrace(
        diagnostic: ManyCandidatesCallDiagnostic,
        tracingStrategy: TracingStrategy,
        trace: BindingTrace,
        context: BasicCallResolutionContext
    ): ManyCandidates<D> {
        // 将诊断中的候选调用转换为解析调用，并记录相关的错误信息
        val resolvedCalls = diagnostic.candidates.map {
            cangjieToResolvedCallTransformer.onlyTransform<D>(
                it.resolvedCall, it.diagnostics + it.getSystem().errors.asDiagnostics()
            )
        }

        // 如果所有候选调用都失败了
        if (diagnostic.candidates.areAllFailed()) {
            // 如果所有失败的调用都是因为接收者不适用
            if (diagnostic.candidates.areAllFailedWithInapplicableWrongReceiver()) {
                tracingStrategy.unresolvedReferenceWrongReceiver(trace, resolvedCalls)
            } else {
                // 记录所有不适用的调用
                tracingStrategy.noneApplicable(trace, resolvedCalls)
                // 记录歧义
                tracingStrategy.recordAmbiguity(trace, resolvedCalls)
            }
        } else {
            // 记录歧义
            tracingStrategy.recordAmbiguity(trace, resolvedCalls)
            // 如果调用的参数都已解析
            if (!context.call.hasUnresolvedArguments(context)) {
                // 如果所有解析调用都是不完整的
                if (resolvedCalls.allIncomplete) {
                    tracingStrategy.cannotCompleteResolve(trace, resolvedCalls)
                } else {
                    // 记录歧义
                    tracingStrategy.ambiguity(trace, resolvedCalls)
                }
            }
        }
        // 返回包含已解析调用的 ManyCandidates 对象
        return ManyCandidates(resolvedCalls)
    }

    private val List<ResolvedCall<*>>.allIncomplete: Boolean get() = all { it.status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE }

    private fun ResolvedCall<*>.recordEffects(trace: BindingTrace) {
//        val moduleDescriptor = DescriptorUtils.getContainingModule(this.resultingDescriptor?.containingDeclaration ?: return)
//        recordLambdasInvocations(trace, moduleDescriptor)
//        recordResultInfo(trace, moduleDescriptor)
    }

    inner class ASTScopeTower(
        val context: BasicCallResolutionContext,
        cjExpression: CjExpression? = null
    ) : ImplicitScopeTower {

        private val cache = HashMap<ReceiverParameterDescriptor, ReceiverValueWithSmartCastInfo>()

        override val lexicalScope: LexicalScope get() = context.scope
        override val areContextReceiversEnabled: Boolean
            get() = context.languageVersionSettings.supportsFeature(
                LanguageFeature.ContextReceivers
            )
        override val syntheticScopes: SyntheticScopes get() = this@PSICallResolver.syntheticScopes
        override val dynamicScope: MemberScope =
            dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)

        override fun getContextReceivers(scope: LexicalScope): List<ReceiverValueWithSmartCastInfo> =
            scope.contextReceiversGroup.map { cache.getOrPut(it) { context.transformToReceiverWithSmartCastInfo(it.value) } }

        override val typeApproximator: TypeApproximator get() = this@PSICallResolver.typeApproximator

        // 默认使用改进的类型推断系统
        override val isNewInferenceEnabled: Boolean
            get() = true


        override fun interceptVariableCandidates(
            resolutionScope: ResolutionScope,
            name: Name,
            initialResults: Collection<VariableDescriptor>,
            location: LookupLocation,
            dispatchReceiver: ReceiverValueWithSmartCastInfo?,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): Collection<VariableDescriptor> {
            return candidateInterceptor.interceptVariableCandidates(
                initialResults,
                this,
                context,
                resolutionScope,
                this@PSICallResolver,
                name,
                location,
                dispatchReceiver,
                extensionReceiver
            )
        }

        override fun interceptFunctionCandidates(
            resolutionScope: ResolutionScope,
            name: Name,
            initialResults: Collection<FunctionDescriptor>,
            location: LookupLocation,
            dispatchReceiver: ReceiverValueWithSmartCastInfo?,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): Collection<FunctionDescriptor> {
            return candidateInterceptor.interceptFunctionCandidates(
                initialResults,
                this,
                context,
                resolutionScope,
                this@PSICallResolver,
                name,
                location,
                dispatchReceiver,
                extensionReceiver
            )
        }

        override val location: LookupLocation =
            cjExpression?.createLookupLocation() ?: context.call.createLookupLocation()
        override val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter get() = this@PSICallResolver.implicitsResolutionFilter

        override fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo? {
            val implicitReceiver = scope.implicitReceiver ?: return null

            return cache.getOrPut(implicitReceiver) {
                context.transformToReceiverWithSmartCastInfo(implicitReceiver.value)
            }
        }

    }

    private fun createResolutionCallbacks(context: BasicCallResolutionContext) =
        createResolutionCallbacks(context.trace, context.inferenceSession, context)

    fun createResolutionCallbacks(
        trace: BindingTrace,
        inferenceSession: InferenceSession,
        context: BasicCallResolutionContext
    ) =
        CangJieResolutionCallbacksImpl(
            trace,
            expressionTypingServices,
            typeApproximator,
            argumentTypeResolver,
            languageVersionSettings,
            cangjieToResolvedCallTransformer,
            dataFlowValueFactory,
            inferenceSession,
            constantExpressionEvaluator,
            typeResolver,
            this,
            postponedArgumentsAnalyzer,
            cangjieConstraintSystemCompleter,
            callComponents,
            doubleColonExpressionResolver,
            deprecationResolver,
            moduleDescriptor,
            context,
            missingSupertypesResolver,
            cangjieCallResolver,
            resultTypeResolver
        )

    private fun resolveReceiver(
        context: BasicCallResolutionContext,
        oldReceiver: Receiver?,
        isSafeCall: Boolean,
        isForImplicitInvoke: Boolean
    ): ReceiverCangJieCallArgument? {
        return when (oldReceiver) {
            null -> null

            is QualifierReceiver -> QualifierReceiverCangJieCallArgument(oldReceiver) // todo report warning if isSafeCall

            is ReceiverValue -> {
                if (oldReceiver is ExpressionReceiver) {
                    val cjExpression =
                        CjPsiUtil.getLastElementDeparenthesized(oldReceiver.expression, context.statementFilter)

                    val bindingContext = context.trace.bindingContext
                    val call = cjExpression?.let {
                        bindingContext[DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL, it]
                    }

                        ?: cjExpression?.getCall(bindingContext)

                    val partiallyResolvedCall =
                        call?.let { bindingContext.get(ONLY_RESOLVED_CALL, it)?.result }

                    if (partiallyResolvedCall != null) {
                        val receiver = ReceiverValueWithSmartCastInfo(oldReceiver, emptySet(), isStable = true)
                        return SubCangJieCallArgumentImpl(
                            CallMaker.makeExternalValueArgument(oldReceiver.expression),
                            context.dataFlowInfo, context.dataFlowInfo, receiver, partiallyResolvedCall
                        )
                    }
                }

                ReceiverExpressionCangJieCallArgument(
                    context.transformToReceiverWithSmartCastInfo(oldReceiver),
                    isSafeCall,
                    isForImplicitInvoke
                )
            }

            else -> error("Incorrect receiver: $oldReceiver")
        }
    }


    private fun resolveArgumentsInParenthesis(
        context: BasicCallResolutionContext,
        arguments: List<ValueArgument>,
        isSpecialFunction: Boolean,
        tracingStrategy: TracingStrategy,
    ): List<CangJieCallArgument> {
        val dataFlowInfoForArguments = context.dataFlowInfoForArguments
        return arguments.map { argument ->
            resolveValueArgument(
                argument.getArgumentExpression()?.getScope(context.trace.bindingContext)
                    ?.let { context.replaceScope(it) } ?: context,
                dataFlowInfoForArguments.getInfo(argument),
                argument,
                isSpecialFunction,
                tracingStrategy
            ).also { resolvedArgument ->
                dataFlowInfoForArguments.updateInfo(argument, resolvedArgument.dataFlowInfoAfterThisArgument)
            }
        }
    }

    /**
     * 为Invoke调用解析调度接收者
     *
     * 本函数旨在处理Invoke调用方式的接收者解析对于非Invoke调用或无效的调用类型，函数将返回null
     * 主要关注于ImplicitInvoke调用的接收者解析，确保调用的正确处理和执行
     *
     * @param context 解析上下文，包含呼叫解析所需的基本信息和参数
     * @param cangjieCallKind 调用的种类，用于识别调用类型是否为Invoke
     * @param oldCall 原始调用对象，仅适用于ImplicitInvoke类型的调用
     * @return 返回解析后的接收者对象，如果解析失败或不适用则返回null
     */
    private fun resolveDispatchReceiverForInvoke(
        context: BasicCallResolutionContext,
        cangjieCallKind: CangJieCallKind,
        oldCall: Call
    ): ReceiverCangJieCallArgument? {

        // 当调用种类不是Invoke时，直接返回null，无需进一步处理
        if (cangjieCallKind != CangJieCallKind.INVOKE) return null

        // 确保oldCall是CallForImplicitInvoke类型，否则抛出异常，表明调用类型错误
        require(oldCall is CallTransformer.CallForImplicitInvoke) { "Call should be CallForImplicitInvoke, but it is: $oldCall" }

        // 调用resolveReceiver函数解析接收者，由于是用于ImplicitInvoke，因此isForImplicitInvoke设置为true
        return resolveReceiver(context, oldCall.dispatchReceiver, isSafeCall = false, isForImplicitInvoke = true)
    }

    private fun resolveTypeArguments(
        context: BasicCallResolutionContext,
        typeArguments: List<CjTypeProjection>
    ): List<TypeArgument> =
        typeArguments.map { projection ->
            if (projection.projectionKind != CjProjectionKind.NONE) {
                context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
            }
            ModifierCheckerCore.check(projection, context.trace, null, languageVersionSettings)

            val typeReference = projection.typeReference ?: return@map TypeArgumentPlaceholder

            if (typeReference.isPlaceholder) {
                val resolvedAnnotations =
                    typeResolver.resolveTypeAnnotations(context.trace, context.scope, typeReference)
                        .apply(ForceResolveUtil::forceResolveAllContents)

                for (annotation in resolvedAnnotations) {
                    val annotationElement = annotation.source.getPsi() ?: continue
                    context.trace.report(
                        UNSUPPORTED.on(
                            annotationElement,
                            "annotations on an underscored type argument"
                        )
                    )
                }

//                if (!arePartiallySpecifiedTypeArgumentsEnabled) {
//                    context.trace.report( UNSUPPORTED.on(typeReference, "underscored type argument"))
//                }

                return@map TypeArgumentPlaceholder
            }

            SimpleTypeArgumentImpl(projection, resolveType(context, typeReference, typeResolver))
        }


    private fun toCangJieCall(
        context: BasicCallResolutionContext,
        cangjieCallKind: CangJieCallKind,
        oldCall: Call,
        name: Name,
        tracingStrategy: TracingStrategy,
        isSpecialFunction: Boolean,
        forcedExplicitReceiver: Receiver? = null,
    ): PSICangJieCallImpl {
        val resolvedExplicitReceiver = resolveReceiver(
            context,
            forcedExplicitReceiver ?: oldCall.explicitReceiver,
            oldCall.isSafeCall(),
            isForImplicitInvoke = false
        )
        val dispatchReceiverForInvoke = resolveDispatchReceiverForInvoke(context, cangjieCallKind, oldCall)

        val resolvedTypeArguments = resolveTypeArguments(context, oldCall.typeArguments).toMutableList()
        val topTypeArguments = if (resolvedExplicitReceiver?.receiver is ClassQualifier) {
            resolveTypeArguments(
                context,
                (resolvedExplicitReceiver.receiver as? ClassQualifier)?.referenceExpression?.getTypeArguments()
                    ?: emptyList()
            )
        } else {
            emptyList()
        }
//            .apply {
//                //        如果存在静态访问行为
////        上一层原子
////        val topTypeArguments = resolvedExplicitReceiver?.receiver
//                if (resolvedExplicitReceiver?.receiver is ClassQualifier) {
////
//
//                    addAll(
//                        resolveTypeArguments(context,
//                            (resolvedExplicitReceiver.receiver as? ClassQualifier)?.referenceExpression?.getTypeArguments()
//                                ?: emptyList()
//                        )
//                    )
//                }
//            }

        val lambdasOutsideParenthesis = oldCall.functionLiteralArguments.size
        val extraArgumentsNumber =
            if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) 1 else lambdasOutsideParenthesis

        val allValueArguments = oldCall.valueArguments
        val argumentsInParenthesis =
            if (extraArgumentsNumber == 0) allValueArguments else allValueArguments.dropLast(extraArgumentsNumber)

        val externalLambdaArguments = oldCall.functionLiteralArguments
        val resolvedArgumentsInParenthesis =
            resolveArgumentsInParenthesis(context, argumentsInParenthesis, isSpecialFunction, tracingStrategy)

        val externalArgument = if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) {
            assert(externalLambdaArguments.isEmpty()) {
                "Unexpected lambda parameters for call $oldCall"
            }
            if (allValueArguments.isEmpty()) {
                throw CangJieExceptionWithAttachmentsImpl("Can not find an external argument for 'set' method")
                    .withPsiAttachment("callElement.cj", oldCall.callElement)
                    .withPsiAttachment("file.cj", oldCall.callElement.takeIf { it.isValid }?.containingFile)
            }
            allValueArguments.last()
        } else {
            if (externalLambdaArguments.size > 1) {
                for (i in externalLambdaArguments.indices) {
                    if (i == 0) continue
                    val lambdaExpression = externalLambdaArguments[i].getLambdaExpression() ?: continue

                    if (lambdaExpression.isTrailingLambdaOnNewLIne) {
                        context.trace.report(UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(lambdaExpression))
                    }
                    context.trace.report(MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(lambdaExpression))
                }
            }

            externalLambdaArguments.firstOrNull()
        }

        val dataFlowInfoAfterArgumentsInParenthesis =
            if (externalArgument != null && resolvedArgumentsInParenthesis.isNotEmpty())
                resolvedArgumentsInParenthesis.last().psiCallArgument.dataFlowInfoAfterThisArgument
            else
                context.dataFlowInfoForArguments.resultInfo

        val resolvedExternalArgument = externalArgument?.let {
            resolveValueArgument(
                context,
                dataFlowInfoAfterArgumentsInParenthesis,
                it,
                isSpecialFunction,
                tracingStrategy
            )
        }
        val resultDataFlowInfo =
            resolvedExternalArgument?.dataFlowInfoAfterThisArgument ?: dataFlowInfoAfterArgumentsInParenthesis

        resolvedArgumentsInParenthesis.forEach { it.setResultDataFlowInfoIfRelevant(resultDataFlowInfo) }
        resolvedExternalArgument?.setResultDataFlowInfoIfRelevant(resultDataFlowInfo)

        val isForImplicitInvoke = oldCall is CallTransformer.CallForImplicitInvoke

        return PSICangJieCallImpl(
            cangjieCallKind,
            oldCall,
            tracingStrategy,
            resolvedExplicitReceiver,
            dispatchReceiverForInvoke,
            name,
            resolvedTypeArguments,
            topTypeArguments,
            resolvedArgumentsInParenthesis,
            resolvedExternalArgument,
            context.dataFlowInfo,
            resultDataFlowInfo,
            context.dataFlowInfoForArguments,
            isForImplicitInvoke
        )
    }

    private fun BasicCallResolutionContext.expandContextForCatchClause(cjExpression: Any): BasicCallResolutionContext {
        if (cjExpression !is CjExpression) return this

        val redeclarationChecker = expressionTypingServices.createLocalRedeclarationChecker(trace)

        var tryOrCatchScope = with(scope) {
            LexicalWritableScope(this, ownerDescriptor, false, redeclarationChecker, LexicalScopeKind.CATCH)
        }
        val tryVariableDescriptorHolder = trace.bindingContext[NEW_INFERENCE_TRY_EXCEPTION_PARAMETER, cjExpression]

        if (tryVariableDescriptorHolder != null) {
            tryOrCatchScope = with(scope) {
                LexicalWritableScope(this, ownerDescriptor, false, redeclarationChecker, LexicalScopeKind.TRY)
            }
            val variableDescriptors = tryVariableDescriptorHolder.get()
            tryVariableDescriptorHolder.set(null)
            variableDescriptors.forEach {
                if (!it.isUnderscoreNamed /*|| !isReferencingToUnderscoreNamedParameterForbidden*/) {
                    tryOrCatchScope.addVariableDescriptor(it)
                }
            }
            return replaceScope(tryOrCatchScope)
        }

        val variableDescriptorHolder =
            trace.bindingContext[NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER, cjExpression] ?: return this
        val variableDescriptor = variableDescriptorHolder.get() ?: return this
        variableDescriptorHolder.set(null)


//        val isReferencingToUnderscoreNamedParameterForbidden =
//            languageVersionSettings.getFeatureSupport(LanguageFeature.ForbidReferencingToUnderscoreNamedParameterOfCatchBlock) == LanguageFeature.State.ENABLED
        if (!variableDescriptor.isUnderscoreNamed /*|| !isReferencingToUnderscoreNamedParameterForbidden*/) {
            tryOrCatchScope.addVariableDescriptor(variableDescriptor)
        }




        return replaceScope(tryOrCatchScope)
    }

    private fun resolveValueArgument(
        outerCallContext: BasicCallResolutionContext,
        startDataFlowInfo: DataFlowInfo,
        valueArgument: ValueArgument,
        isSpecialFunction: Boolean,
        tracingStrategy: TracingStrategy,
    ): PSICangJieCallArgument {
        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns

        fun createParseErrorElement() = ParseErrorCangJieCallArgument(valueArgument, startDataFlowInfo)

        val argumentExpression = valueArgument.getArgumentExpression() ?: return createParseErrorElement()
        val cjExpression = CjPsiUtil.deparenthesize(argumentExpression) ?: createParseErrorElement()

        val argumentName = valueArgument.getArgumentName()?.asName

        @Suppress("NAME_SHADOWING")
        val outerCallContext = outerCallContext.expandContextForCatchClause(cjExpression)

        processFunctionalExpression(
            outerCallContext, argumentExpression, startDataFlowInfo,
            valueArgument, argumentName, builtIns, typeResolver
        )?.let {
            return it
        }
        if (cjExpression is CjCasePattern) {
            return CasePatternCangJieCallArgumentImpl(
                valueArgument, argumentName, startDataFlowInfo, startDataFlowInfo, cjExpression, outerCallContext
            )
        }
        if (cjExpression is CjCollectionLiteralExpression) {
            return CollectionLiteralCangJieCallArgumentImpl(
                valueArgument, argumentName, startDataFlowInfo, startDataFlowInfo, cjExpression, outerCallContext
            )
        }

        val context = outerCallContext.replaceContextDependency(ContextDependency.DEPENDENT)
            .replaceDataFlowInfo(startDataFlowInfo)
            .let {
                if (isSpecialFunction &&
                    argumentExpression is CjBlockExpression &&
                    ArgumentTypeResolver.getCallableReferenceExpressionIfAny(argumentExpression, it) != null
                ) {
                    it
                } else {
                    it.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
                }
            }

//        if (cjExpression is CjCallableReferenceExpression) {
//            return createCallableReferenceCangJieCallArgument(
//                context, cjExpression, startDataFlowInfo, valueArgument, argumentName, outerCallContext, tracingStrategy
//            )
//        }


        // argumentExpression instead of cjExpression is hack -- type info should be stored also for parenthesized expression
        val tempTrace = TemporaryBindingTrace.create(
            context.trace, "trace for resolving argument expression"
        )
        val typeInfo = expressionTypingServices.getTypeInfo(argumentExpression, context.replaceBindingTrace(tempTrace))
        if (typeInfo.type == null) {

            if (argumentExpression is CjNameReferenceExpression) {
                if (tempTrace[IS_FUNC, argumentExpression] == true) {
                    return createCallableReferenceCangJieCallArgument(
                        context,
                        argumentExpression,
                        startDataFlowInfo,
                        valueArgument,
                        argumentName,
                        outerCallContext,
                        tracingStrategy
                    )

                }
            } else if (argumentExpression is CjDotQualifiedExpression && argumentExpression.selectorExpression is CjNameReferenceExpression) {
                if (tempTrace[IS_FUNC, argumentExpression.selectorExpression as CjNameReferenceExpression] == true) {
                    return createCallableReferenceCangJieCallArgument(
                        context,
                        argumentExpression,
                        startDataFlowInfo,
                        valueArgument,
                        argumentName,
                        outerCallContext,
                        tracingStrategy
                    )

                }
            }

        } else if (typeInfo.type.isFunctionType) {
            if (argumentExpression is CjNameReferenceExpression) {
                if (tempTrace[IS_FUNC, argumentExpression] == true) {
                    return createCallableReferenceCangJieCallArgument(
                        context,
                        argumentExpression,
                        startDataFlowInfo,
                        valueArgument,
                        argumentName,
                        outerCallContext,
                        tracingStrategy
                    )

                }
            } else if (argumentExpression is CjDotQualifiedExpression && argumentExpression.selectorExpression is CjNameReferenceExpression) {
                if (tempTrace[IS_FUNC, argumentExpression.selectorExpression as CjNameReferenceExpression] == true) {
                    return createCallableReferenceCangJieCallArgument(
                        context,
                        argumentExpression,
                        startDataFlowInfo,
                        valueArgument,
                        argumentName,
                        outerCallContext,
                        tracingStrategy
                    )

                }
            }
        }
        tempTrace.commit()
        return createSimplePSICallArgument(context, valueArgument, typeInfo) ?: createParseErrorElement()
    }

    fun getLhsResult(context: BasicCallResolutionContext, cjExpression: CjCallableReference): LHSResult {
        val expressionTypingContext = ExpressionTypingContext.newContext(context)
        val doubleColonLhs = doubleColonExpressionResolver.resolveDoubleColonLHS(cjExpression, expressionTypingContext)
            ?: return LHSResult.Empty
        return when (doubleColonLhs) {
            is DoubleColonLHS.Expression -> {

                val fakeArgument = FakeValueArgumentForLeftCallableReference(cjExpression)

                val cangjieCallArgument = createSimplePSICallArgument(context, fakeArgument, doubleColonLhs.typeInfo)
                cangjieCallArgument?.let { LHSResult.Expression(it as SimpleCangJieCallArgument) } ?: LHSResult.Error
            }

            is DoubleColonLHS.Type -> {
                val qualifiedExpression = cjExpression.receiverExpression!!
                val qualifier = expressionTypingContext.trace.get(QUALIFIER, qualifiedExpression)
                val classifier = doubleColonLhs.type.constructor.declarationDescriptor
                if (classifier !is ClassDescriptor) {
                    expressionTypingContext.trace.report(CALLABLE_REFERENCE_LHS_NOT_A_CLASS.on(cjExpression))
                    LHSResult.Error
                } else {
                    LHSResult.Type(qualifier, doubleColonLhs.type.unwrap())
                }
            }


        }


    }


    private fun NewResolutionOldInference.ResolutionKind.toCangJieCallKind(): CangJieCallKind =
        when (this) {
            is NewResolutionOldInference.ResolutionKind.Function -> CangJieCallKind.FUNCTION
            is NewResolutionOldInference.ResolutionKind.Variable -> CangJieCallKind.VARIABLE
            is NewResolutionOldInference.ResolutionKind.Invoke -> CangJieCallKind.INVOKE
//            is NewResolutionOldInference.ResolutionKind.Enum -> CangJieCallKind.ENUM
            is NewResolutionOldInference.ResolutionKind.EnumEntry -> CangJieCallKind.ENUM_ENTRY

            is NewResolutionOldInference.ResolutionKind.CaseEnum -> CangJieCallKind.CASE_ENUM

            is NewResolutionOldInference.ResolutionKind.CallableReference -> CangJieCallKind.CALLABLE_REFERENCE
            is NewResolutionOldInference.ResolutionKind.GivenCandidates -> CangJieCallKind.UNSUPPORTED
        }

    private fun refineNameForRemOperator(isBinaryRemOperator: Boolean, name: Name): Name {
        return name
//        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
//        return if (isBinaryRemOperator && !shouldUseOperatorRem) OperatorConventions.REM_TO_MOD_OPERATION_NAMES[name]!! else name
    }

    private fun calculateExpectedType(context: BasicCallResolutionContext): UnwrappedType? {
        val expectedType = context.expectedType.unwrap()

        return if (context.contextDependency == ContextDependency.DEPENDENT) {
            if (TypeUtils.noExpectedType(expectedType)) null else expectedType
        } else {
            if (expectedType.isError) TypeUtils.NO_EXPECTED_TYPE else expectedType
        }
    }

    fun <D : CallableDescriptor> runResolutionAndInference(
        context: BasicCallResolutionContext,
        name: Name,
        resolutionKind: NewResolutionOldInference.ResolutionKind,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {

        val isBinaryRemOperator = isBinaryRemOperator(context.call)
        val refinedName = refineNameForRemOperator(isBinaryRemOperator, name)
//
        val cangjieCallKind = resolutionKind.toCangJieCallKind()
//        if (cangjieCallKind == CangJieCallKind.FUNCTION && context.call is CallMaker.CallImpl) {
//            cangjieCallKind = CangJieCallKind.CALLABLE_REFERENCE
//        }
        val cangjieCall = toCangJieCall(
            context,
            cangjieCallKind,
            context.call,
            refinedName,
            tracingStrategy,
            isSpecialFunction = false
        )
        val scopeTower = ASTScopeTower(context)
        val resolutionCallbacks = createResolutionCallbacks(context)
//
        val expectedType = calculateExpectedType(context)
        val result = cangjieCallResolver.resolveAndCompleteCall(
            scopeTower, resolutionCallbacks, cangjieCall, expectedType, context.collectAllCandidates
        )

//        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
//        if (isBinaryRemOperator && shouldUseOperatorRem && (result.isEmpty() || result.areAllInapplicable())) {
//            result = resolveToDeprecatedMod(name, context, cangjieCallKind, tracingStrategy, scopeTower, resolutionCallbacks, expectedType)
//        }
//
        if (result.isEmpty() && reportAdditionalDiagnosticIfNoCandidates(
                context,
                scopeTower,
                cangjieCallKind,
                cangjieCall
            )
        ) {
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        val overloadResolutionResults = convertToOverloadResolutionResults<D>(context, result, tracingStrategy)

        return overloadResolutionResults.also {
            clearCacheForApproximationResults()
            checkCallWithAdditionalResolve(it, scopeTower, resolutionCallbacks, expectedType, context)
        }
    }

    private fun CallResolutionResult.isEmpty(): Boolean =
        diagnostics.firstIsInstanceOrNull<NoneCandidatesCallDiagnostic>() != null

    private fun <D : CallableDescriptor> checkCallWithAdditionalResolve(
        overloadResolutionResults: OverloadResolutionResults<D>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext,
    ) {
        for (callChecker in callCheckersWithAdditionalResolve) {
            callChecker.check(overloadResolutionResults, scopeTower, resolutionCallbacks, expectedType, context)
        }
    }

    // true if we found something
    private fun reportAdditionalDiagnosticIfNoCandidates(
        context: BasicCallResolutionContext,
        scopeTower: ImplicitScopeTower,
        kind: CangJieCallKind,
        cangjieCall: CangJieCall
    ): Boolean {
        val reference = context.call.calleeExpression as? CjReferenceExpression ?: return false

        val errorCandidates = when (kind) {
            CangJieCallKind.FUNCTION ->
                collectErrorCandidatesForFunction(scopeTower, cangjieCall.name, cangjieCall.explicitReceiver?.receiver)

            CangJieCallKind.VARIABLE ->
                collectErrorCandidatesForVariable(scopeTower, cangjieCall.name, cangjieCall.explicitReceiver?.receiver)

            else -> emptyList()
        }

        for (candidate in errorCandidates) {
            if (candidate is ErrorCandidate.Classifier) {
                context.trace.record(REFERENCE_TARGET, reference, candidate.descriptor)
                context.trace.report(
                    RESOLUTION_TO_CLASSIFIER.on(
                        reference,
                        candidate.descriptor,
                        candidate.kind,
                        candidate.errorMessage
                    )
                )
                return true
            }
        }
        return false
    }

    private fun clearCacheForApproximationResults() {
        // Mostly, we approximate captured or some other internal types that don't live longer than resolve for a call,
        // so it's quite useless to preserve cache for longer time
//        typeApproximator.clearCache()
    }
//
//    private fun <D : CallableDescriptor> checkCallWithAdditionalResolve(
//        overloadResolutionResults: OverloadResolutionResults<D>,
//        scopeTower: ImplicitScopeTower,
//        resolutionCallbacks: CangJieResolutionCallbacks,
//        expectedType: UnwrappedType?,
//        context: BasicCallResolutionContext,
//    ) {
//        for (callChecker in callCheckersWithAdditionalResolve) {
//            callChecker.check(overloadResolutionResults, scopeTower, resolutionCallbacks, expectedType, context)
//        }
//    }
//    private fun clearCacheForApproximationResults() {
//        // Mostly, we approximate captured or some other internal types that don't live longer than resolve for a call,
//        // so it's quite useless to preserve cache for longer time
//        typeApproximator.clearCache()
//    }
}


fun CjElement.getScope(context: BindingContext): LexicalScope? {
    return context[LEXICAL_SCOPE, this]


}
