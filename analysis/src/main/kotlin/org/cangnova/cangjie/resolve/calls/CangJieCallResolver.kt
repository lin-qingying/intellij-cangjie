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


import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.cangnova.cangjie.resolve.calls.components.*
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.context.CheckArgumentTypesMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.model.CangJieCallKind.*
import org.cangnova.cangjie.resolve.calls.tower.*
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.UnwrappedType

/**
 * 仓颉调用解析器
 *
 * 负责解析函数调用、属性访问、可调用引用等各种调用表达式。
 *
 * ## 类型推断模式
 *
 * 支持两种类型推断模式：
 * 1. **单轮推断（默认）**: 类似 Kotlin 的单轮约束求解
 * 2. **迭代推断（实验性）**: 编译器风格的多轮迭代推断
 *
 * 迭代推断模式可以通过设置 `enableIterativeInference` 标志启用。
 * 迭代推断更适合处理复杂的类型推导场景，如：
 * - Lambda 参数的延迟推导
 * - Option 类型的自动装箱
 * - 显式类型参数与隐式推导的混合
 *
 * @property towerResolver 塔式解析器，用于候选项收集
 * @property cangjieCallCompleter 调用完成器，用于类型推断和完成
 * @property overloadingConflictResolver 重载冲突解析器
 * @property callableReferenceArgumentResolver 可调用引用参数解析器
 * @property callComponents 调用组件集合
 */
class CangJieCallResolver(
    private val towerResolver: TowerResolver,
    private val cangjieCallCompleter: CangJieCallCompleter,
    private val overloadingConflictResolver: OverloadingConflictResolver,
    private val callableReferenceArgumentResolver: CallableReferenceArgumentResolver,
    private val callComponents: CangJieCallComponents
) {
    /**
     * 是否启用迭代类型推断
     *
     * 实验性功能：启用编译器风格的迭代多轮类型推断。
     *
     * TODO: 当迭代推断稳定后，将此标志移除并默认启用迭代推断。
     * TODO: 可以通过语言版本设置或编译器标志来控制此行为。
     */
    private val enableIterativeInference: Boolean = false // TODO: 从配置读取

    // TODO: 迭代推断引擎集成点
    // 当 enableIterativeInference = true 时，应该：
    // 1. 在 cangjieCallCompleter.runCompletion 之前或内部
    // 2. 创建 IterativeTypeInferenceEngine 实例
    // 3. 使用迭代引擎进行类型推断
    // 4. 将推断结果应用到约束系统
    //
    // 示例代码:
    // ```kotlin
    // if (enableIterativeInference && shouldUseIterativeInference(candidates)) {
    //     val engine = IterativeTypeInferenceEngine(
    //         constraintSystemBuilder = ...,
    //         completer = callComponents.constraintSystemCompleter
    //     )
    //     val result = engine.runIterativeInference(
    //         typeVariables = extractTypeVariables(candidates),
    //         arguments = buildArgumentStates(cangjieCall, candidates),
    //         maxIterations = 10
    //     )
    //     if (result.isSuccess) {
    //         applySolution(result.solution)
    //     }
    // }
    // ```
    fun resolveCallableReferenceArgument(
        argument: CallableReferenceCangJieCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): Collection<CallableReferenceResolutionCandidate> {
        val scopeTower = callComponents.statelessCallbacks.getScopeTowerForCallableReferenceArgument(argument)
        val factory = createCallableReferenceCallFactory(
            scopeTower,
            argument.call,
            resolutionCallbacks,
            expectedType,
            argument,
            baseSystem
        )

        return resolveCall(scopeTower, resolutionCallbacks, argument.call, collectAllCandidates = false, factory)
    }

    fun resolveAndCompleteGivenCandidates(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        cangjieCall: CangJieCall,
        expectedType: UnwrappedType?,
        givenCandidates: Collection<GivenCandidate>,
        collectAllCandidates: Boolean
    ): CallResolutionResult {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        cangjieCall.checkCallInvariants()

        val candidateFactory = SimpleCandidateFactory(callComponents, scopeTower, cangjieCall, resolutionCallbacks)
        val resolutionCandidates = givenCandidates.map { candidateFactory.createCandidate(it).forceResolution() }

        if (collectAllCandidates) {
            val allCandidates = towerResolver.runWithEmptyTowerData(
                KnownResultProcessor(resolutionCandidates),
                TowerResolver.AllCandidatesCollector(),
                useOrder = false
            )
            return cangjieCallCompleter.createAllCandidatesResult(allCandidates, expectedType, resolutionCallbacks)

        }

        val candidates = towerResolver.runWithEmptyTowerData(
            KnownResultProcessor(resolutionCandidates),
            TowerResolver.SuccessfulResultCollector(),
            useOrder = true
        )
        val mostSpecificCandidates = choseMostSpecific(cangjieCall, resolutionCallbacks, candidates)

        return cangjieCallCompleter.runCompletion(
            candidateFactory,
            mostSpecificCandidates.toMutableSet(),
            expectedType,
            resolutionCallbacks
        )
    }

    private fun createCallableReferenceCallFactory(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?,
        argument: CallableReferenceCangJieCallArgument? = null,
        baseSystem: ConstraintStorage? = null
    ): CandidateFactory<CallableReferenceResolutionCandidate> {
        val resolutionAtom = argument
            ?: CallableReferenceCangJieCall(
                cangjieCall,
                resolutionCallbacks.getLhsResult(cangjieCall),
                cangjieCall.name
            )

        return CallableReferencesCandidateFactory(
            resolutionAtom,
            callComponents,
            scopeTower,
            expectedType,
            baseSystem,
            resolutionCallbacks
        )
    }

    private fun createSimpleCallFactory(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall,
        resolutionCallbacks: CangJieResolutionCallbacks,
    ): CandidateFactory<ResolutionCandidate> =
        SimpleCandidateFactory(callComponents, scopeTower, cangjieCall, resolutionCallbacks)

    private fun createFactory(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?
    ): CandidateFactory<ResolutionCandidate> =
        when (cangjieCall.callKind) {
            CALLABLE_REFERENCE -> createCallableReferenceCallFactory(
                scopeTower,
                cangjieCall,
                resolutionCallbacks,
                expectedType
            )

            else -> createSimpleCallFactory(scopeTower, cangjieCall, resolutionCallbacks)
        }

    private fun <C : ResolutionCandidate> resolveCall(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        cangjieCall: CangJieCall,
        collectAllCandidates: Boolean,
        candidateFactory: CandidateFactory<C>,
    ): Collection<C> {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        cangjieCall.checkCallInvariants()

        val processor = when (cangjieCall.callKind) {



            VARIABLE -> {
                createVariableProcessor(
                    scopeTower,
                    cangjieCall.name,
                    candidateFactory,
                    cangjieCall.explicitReceiver?.receiver
                )
            }

            ENUM_CONSTRUCTOR -> createEnumConstructorProcessor(
                scopeTower,
                cangjieCall.name,
                candidateFactory,
                resolutionCallbacks.getCandidateFactoryForInvoke(scopeTower, cangjieCall),
                cangjieCall.explicitReceiver?.receiver
            )

            FUNCTION -> {
                createFunctionProcessor(
                    scopeTower,
                    cangjieCall.name,
                    candidateFactory,
                    resolutionCallbacks.getCandidateFactoryForInvoke(scopeTower, cangjieCall),
                    cangjieCall.explicitReceiver?.receiver
                )
            }

            CALLABLE_REFERENCE -> {
                createCallableReferenceProcessor(candidateFactory as CallableReferencesCandidateFactory) as ScopeTowerProcessor<C>
            }

            INVOKE -> {
                createProcessorWithReceiverValueOrEmpty(cangjieCall.explicitReceiver?.receiver) {
                    createCallTowerProcessorForExplicitInvoke(
                        scopeTower,
                        candidateFactory,
                        cangjieCall.dispatchReceiverForInvokeExtension?.receiver as ReceiverValueWithSmartCastInfo,
                        it
                    )
                }
            }

            UNSUPPORTED -> throw UnsupportedOperationException()

            CASE_ENUM -> TODO()
        }

//        if (collectAllCandidates) {
//            return towerResolver.collectAllCandidates(scopeTower, processor, cangjieCall.name)
//        }

        val candidates = towerResolver.runResolve(
            scopeTower,
            processor,
            useOrder = cangjieCall.callKind != UNSUPPORTED,
            name = cangjieCall.name
        )

        @Suppress("UNCHECKED_CAST")
        return choseMostSpecific(cangjieCall, resolutionCallbacks, candidates) as Set<C>
    }

    private fun choseMostSpecific(
        cangjieCall: CangJieCall,
        resolutionCallbacks: CangJieResolutionCallbacks,
        candidates: Collection<ResolutionCandidate>
    ): Set<ResolutionCandidate> {
        // 默认启用：使用改进的SAM适配器优先级（不再过滤合成描述符）
        var refinedCandidates = candidates

        var maximallySpecificCandidates =
            if (cangjieCall.callKind == CALLABLE_REFERENCE) {
                @Suppress("UNCHECKED_CAST")
                overloadingConflictResolver.chooseMaximallySpecificCandidates(
                    refinedCandidates,
                    CheckArgumentTypesMode.CHECK_CALLABLE_TYPE,
                    discriminateGenerics = false
                )
            } else {
                overloadingConflictResolver.chooseMaximallySpecificCandidates(
                    refinedCandidates,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    discriminateGenerics = true // todo
                )
            }

        if (maximallySpecificCandidates.size > 1) {
            if (maximallySpecificCandidates.size == 2) {
                // 枚举构造器现在直接通过 EnumConstructorDescriptor 暴露
                val enumEntryCandidate = maximallySpecificCandidates.find {
                    val descriptor = it.resolvedCall.candidateDescriptor
                    descriptor is EnumConstructorDescriptor
                }
                if (enumEntryCandidate != null) {
                    val otherCandidate = maximallySpecificCandidates.find {
                        val candidateDescriptor = it.resolvedCall.candidateDescriptor
                        candidateDescriptor !is EnumConstructorDescriptor
                    }
                    if (otherCandidate != null) {

                    }
                }
            }
//            if (
//                callComponents.languageVersionSettings.supportsFeature(LanguageFeature.OverloadResolutionByLambdaReturnType)   &&
//                cangjieCall.callKind != CangJieCallKind.CALLABLE_REFERENCE &&
//                candidates.all { it.isSuccessful } &&
//                candidates.all { resolutionCallbacks.inferenceSession.shouldRunCompletion(it) }
//            ) {
//                val candidatesWithAnnotation = candidates.filter {
//                    it.resolvedCall.descriptor.annotations.hasAnnotation(OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_FQ_NAME)
//                }.toSet()
//                val candidatesWithoutAnnotation = candidates - candidatesWithAnnotation
//                if (candidatesWithAnnotation.isNotEmpty()) {
//                    @Suppress("UNCHECKED_CAST")
//                    val newCandidates = cangjieCallCompleter.chooseCandidateRegardingOverloadResolutionByLambdaReturnType(
//                        maximallySpecificCandidates as Set<SimpleResolutionCandidate>,
//                        resolutionCallbacks
//                    )
//                    maximallySpecificCandidates = overloadingConflictResolver.chooseMaximallySpecificCandidates(
//                        newCandidates,
//                        CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
//                        discriminateGenerics = true
//                    )
//
//                    if (maximallySpecificCandidates.size > 1 && candidatesWithoutAnnotation.any { it in maximallySpecificCandidates }) {
//                        maximallySpecificCandidates = maximallySpecificCandidates.toMutableSet().apply { removeAll(candidatesWithAnnotation) }
//                        maximallySpecificCandidates.singleOrNull()?.addDiagnostic(CandidateChosenUsingOverloadResolutionByLambdaAnnotation())
//                    }
//                }
//            }
        }

        return maximallySpecificCandidates

    }

    fun resolveAndCompleteCall(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        cangjieCall: CangJieCall,
        expectedType: UnwrappedType?,
        collectAllCandidates: Boolean,
    ): CallResolutionResult {
        val candidateFactory = createFactory(scopeTower, cangjieCall, resolutionCallbacks, expectedType)
        val candidates =
            resolveCall(scopeTower, resolutionCallbacks, cangjieCall, collectAllCandidates, candidateFactory)

        if (collectAllCandidates) {
            return cangjieCallCompleter.createAllCandidatesResult(candidates, expectedType, resolutionCallbacks)
        }

        return cangjieCallCompleter.runCompletion(
            candidateFactory,
            candidates.toMutableSet(),
            expectedType,
            resolutionCallbacks
        )
    }
}
