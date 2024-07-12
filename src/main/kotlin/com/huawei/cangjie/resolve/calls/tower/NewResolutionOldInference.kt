package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.calls.CallResolver
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.types.TypeApproximator
import com.huawei.cangjie.utils.compactIfPossible

class NewResolutionOldInference(
//    private val candidateResolver: CandidateResolver,
//    private val towerResolver: TowerResolver,
//    private val resolutionResultsHandler: ResolutionResultsHandler,
//    private val dynamicCallableDescriptors: DynamicCallableDescriptors,
//    private val syntheticScopes: SyntheticScopes,
//    private val languageVersionSettings: LanguageVersionSettings,
//    private val builderInferenceSupport: BuilderInferenceSupport,
    private val deprecationResolver: DeprecationResolver,
    private val typeApproximator: TypeApproximator,
//    private val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter,
    private val callResolver: CallResolver,
//    private val candidateInterceptor: CandidateInterceptor
) {

    class MyCandidate(
        // Diagnostics that are already computed
        // if resultingApplicability is successful they must be the same as `diagnostics`,
        // otherwise they might be a bit different but result remains unsuccessful
        val eagerDiagnostics: List<CangJieCallDiagnostic>,
        val resolvedCall: MutableResolvedCall<*>,
        finalDiagnosticsComputation: (() -> List<CangJieCallDiagnostic>)? = null
    ) : Candidate {
        val diagnostics: List<CangJieCallDiagnostic> by lazy(LazyThreadSafetyMode.NONE) {
            finalDiagnosticsComputation?.invoke() ?: eagerDiagnostics
        }

        operator fun component1() = diagnostics
        operator fun component2() = resolvedCall

        override val resultingApplicability: CandidateApplicability by lazy(LazyThreadSafetyMode.NONE) {
            getResultApplicability(diagnostics)
        }

        override fun addCompatibilityWarning(other: Candidate) {
            // Only applicable for new inference
        }

        override val isSuccessful = getResultApplicability(eagerDiagnostics).isSuccess
    }
    sealed class ResolutionKind {
//        abstract internal fun createTowerProcessor(
//            outer: NewResolutionOldInference,
//            name: Name,
//            tracing: TracingStrategy,
//            scopeTower: ImplicitScopeTower,
//            explicitReceiver: DetailedReceiver?,
//            context: BasicCallResolutionContext
//        ): ScopeTowerProcessor<MyCandidate>

        object Function : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                return createFunctionProcessor(
//                    scopeTower,
//                    name,
//                    functionFactory,
//                    outer.CandidateFactoryProviderForInvokeImpl(functionFactory),
//                    explicitReceiver
//                )
//            }
        }

        object Variable : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                return createVariableAndObjectProcessor(scopeTower, name, variableFactory, explicitReceiver)
//            }
        }
//
//        object CallableReference : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                return PrioritizedCompositeScopeTowerProcessor(
//                    createSimpleFunctionProcessor(scopeTower, name, functionFactory, explicitReceiver, classValueReceiver = false),
//                    createVariableProcessor(scopeTower, name, variableFactory, explicitReceiver, classValueReceiver = false)
//                )
//            }
//        }
//
//        object Invoke : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                // todo
//                val call = (context.call as? CallTransformer.CallForImplicitInvoke).sure {
//                    "Call should be CallForImplicitInvoke, but it is: ${context.call}"
//                }
//                return createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
//                    createCallTowerProcessorForExplicitInvoke(
//                        scopeTower,
//                        functionFactory,
//                        context.transformToReceiverWithSmartCastInfo(call.dispatchReceiver),
//                        it
//                    )
//                }
//            }
//
//        }

        class GivenCandidates : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                throw IllegalStateException("Should be not called")
//            }
        }
    }


    fun <D : CallableDescriptor> runResolution(
        context: BasicCallResolutionContext,
        name: Name,
        kind: ResolutionKind,
        tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<D> {
//        TODO runResolution
        return OverloadResolutionResultsImpl.nameNotFound()
    }
}


fun ResolutionContext<*>.transformToReceiverWithSmartCastInfo(receiver: ReceiverValue) =
    transformToReceiverWithSmartCastInfo(scope.ownerDescriptor, trace.bindingContext, dataFlowInfo, receiver, /*languageVersionSettings,*/ dataFlowValueFactory)
fun transformToReceiverWithSmartCastInfo(
    containingDescriptor: DeclarationDescriptor,
    bindingContext: BindingContext,
    dataFlowInfo: DataFlowInfo,
    receiver: ReceiverValue,
//    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory
): ReceiverValueWithSmartCastInfo {
    val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiver, bindingContext, containingDescriptor)
    return ReceiverValueWithSmartCastInfo(
        receiver,
        dataFlowInfo.getCollectedTypes(dataFlowValue/*, languageVersionSettings*/).compactIfPossible(),
        dataFlowValue.isStable
    )
}
