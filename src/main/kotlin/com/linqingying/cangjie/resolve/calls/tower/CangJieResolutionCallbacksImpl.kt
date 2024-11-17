/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.builtins.createFunctionType
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjCallableReference
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjPsiUtil
import com.linqingying.cangjie.psi.CjReturnExpression
import com.linqingying.cangjie.psi.psiUtil.getBinaryWithTypeParent
import com.linqingying.cangjie.psi.psiUtil.lastBlockStatementOrThis
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.calls.ArgumentTypeResolver
import com.linqingying.cangjie.resolve.calls.CangJieCallResolver
import com.linqingying.cangjie.resolve.calls.components.*
import com.linqingying.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.linqingying.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.linqingying.cangjie.resolve.calls.context.ContextDependency
import com.linqingying.cangjie.resolve.calls.inference.BuilderInferenceSession
import com.linqingying.cangjie.resolve.calls.inference.NewConstraintSystem
import com.linqingying.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import com.linqingying.cangjie.resolve.calls.inference.components.ResultTypeResolver
import com.linqingying.cangjie.resolve.calls.inference.components.TypeVariableDirectionCalculator
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.inference.model.NewTypeVariable
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.calls.util.CallMaker
import com.linqingying.cangjie.resolve.constants.CompileTimeConstant
import com.linqingying.cangjie.resolve.constants.IntegerValueTypeConstant
import com.linqingying.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.linqingying.cangjie.resolve.deprecation.DeprecationResolver
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.resolve.descriptorUtil.isFunctionForExpectTypeFromCastFeature
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.receivers.QualifierReceiver
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import com.linqingying.cangjie.types.expressions.ExpressionTypingServices
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo

data class LambdaContextInfo(
    var typeInfo: CangJieTypeInfo? = null,
    var dataFlowInfoAfter: DataFlowInfo? = null,
    var lexicalScope: LexicalScope? = null,
    var trace: BindingTrace? = null
)

class CangJieResolutionCallbacksImpl(
    val trace: BindingTrace,
    private val expressionTypingServices: ExpressionTypingServices,
    private val typeApproximator: TypeApproximator,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val dataFlowValueFactory: DataFlowValueFactory,
    override val inferenceSession: InferenceSession,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val typeResolver: TypeResolver,
    private val psiCallResolver: PSICallResolver,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val callComponents: CangJieCallComponents,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val cangjieCallResolver: CangJieCallResolver,
    private val resultTypeResolver: ResultTypeResolver,
) : CangJieResolutionCallbacks {
    override fun resolveCallableReferenceArgument(
        argument: CallableReferenceCangJieCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage
    ): Collection<CallableReferenceResolutionCandidate> =
        cangjieCallResolver.resolveCallableReferenceArgument(argument, expectedType, baseSystem, this)

    class LambdaInfo(val expectedType: UnwrappedType, val contextDependency: ContextDependency) {
        val returnStatements = ArrayList<Pair<CjReturnExpression, LambdaContextInfo?>>()
        val lastExpressionInfo = LambdaContextInfo()

        companion object {
            val STUB_EMPTY = LambdaInfo(TypeUtils.NO_EXPECTED_TYPE, ContextDependency.INDEPENDENT)
        }
    }

    override fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: LambdaCangJieCallArgument,
        receiverType: UnwrappedType?,
        contextReceiversTypes: List<UnwrappedType>,
        parameters: List<UnwrappedType>,
        expectedReturnType: UnwrappedType?,
        annotations: Annotations,
        stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>
    ): ReturnArgumentsAnalysisResult {
        val psiCallArgument = lambdaArgument.psiCallArgument as PSIFunctionCangJieCallArgument
        val outerCallContext = psiCallArgument.outerCallContext

        fun createCallArgument(
            cjExpression: CjExpression,
            typeInfo: CangJieTypeInfo,
            scope: LexicalScope?,
            newTrace: BindingTrace?
        ): PSICangJieCallArgument? {
            var newContext = outerCallContext
            if (scope != null) newContext = newContext.replaceScope(scope)
            if (newTrace != null) newContext = newContext.replaceBindingTrace(newTrace)

            processFunctionalExpression(
                newContext, cjExpression, typeInfo.dataFlowInfo, CallMaker.makeExternalValueArgument(cjExpression),
                null, outerCallContext.scope.ownerDescriptor.builtIns, typeResolver
            )?.let {
                it.setResultDataFlowInfoIfRelevant(typeInfo.dataFlowInfo)
                return it
            }

            val deparenthesizedExpression = CjPsiUtil.deparenthesize(cjExpression) ?: cjExpression

            return createSimplePSICallArgument(
                trace.bindingContext,
                outerCallContext.statementFilter,
                outerCallContext.scope.ownerDescriptor,
                CallMaker.makeExternalValueArgument(cjExpression),
                DataFlowInfo.EMPTY,
                typeInfo,
                languageVersionSettings,
                dataFlowValueFactory,
                outerCallContext.call
            )
//
//            return if (deparenthesizedExpression is CjCallableReferenceExpression) {
//                psiCallResolver.createCallableReferenceCangJieCallArgument(
//                    newContext, deparenthesizedExpression, DataFlowInfo.EMPTY,
//                    CallMaker.makeExternalValueArgument(deparenthesizedExpression),
//                    argumentName = null,
//                    outerCallContext,
//                    tracingStrategy = TracingStrategyImpl.create(deparenthesizedExpression.callableReference, newContext.call)
//                )
//            } else {
//                createSimplePSICallArgument(
//                    trace.bindingContext, outerCallContext.statementFilter, outerCallContext.scope.ownerDescriptor,
//                    CallMaker.makeExternalValueArgument(cjExpression), DataFlowInfo.EMPTY, typeInfo, languageVersionSettings,
//                    dataFlowValueFactory, outerCallContext.call
//                )
//            }
        }

        val lambdaInfo = LambdaInfo(
            expectedReturnType ?: TypeUtils.NO_EXPECTED_TYPE,
            if (expectedReturnType == null) ContextDependency.DEPENDENT else ContextDependency.INDEPENDENT
        )

        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns

        // We have to refine receiverType because resolve inside lambda needs proper scope from receiver,
        // and for implicit receivers there are no expression which type would've been refined in ExpTypingVisitor
        // Relevant test: multiplatformTypeRefinement/lambdas
        //
        // It doesn't happen in similar cases with other implicit receivers (e.g., with scope of extension receiver
        // inside extension function) because during resolution of types we correctly discriminate headers
        //
        // Also note that refining the whole type might be undesired because sometimes it contains NO_EXPECTED_TYPE
        // which throws exceptions on attempt to call equals
        val refinedReceiverType = receiverType?.let {
            @OptIn(TypeRefinement::class) callComponents.cangjieTypeChecker.cangjieTypeRefiner.refineType(it)
        }
        val refinedContextReceiverTypes = contextReceiversTypes.map {
            @OptIn(TypeRefinement::class) callComponents.cangjieTypeChecker.cangjieTypeRefiner.refineType(it)
        }

        val expectedType = createFunctionType(
            builtIns, annotations, refinedReceiverType, refinedContextReceiverTypes, parameters, null,
            lambdaInfo.expectedType
        )

        val approximatesExpectedType =
            typeApproximator.approximateToSubType(expectedType, TypeApproximatorConfiguration.LocalDeclaration)
                ?: expectedType

        val builderInferenceSession =
            if (stubsForPostponedVariables.isNotEmpty()) {
                BuilderInferenceSession(
                    psiCallResolver,
                    postponedArgumentsAnalyzer,
                    cangjieConstraintSystemCompleter,
                    callComponents,
                    builtIns,
                    topLevelCallContext,
                    stubsForPostponedVariables,
                    trace,
                    cangjieToResolvedCallTransformer,
                    expressionTypingServices,
                    argumentTypeResolver,
                    doubleColonExpressionResolver,
                    deprecationResolver,
                    moduleDescriptor,
                    typeApproximator,
                    missingSupertypesResolver,
                    lambdaArgument
                ).apply { lambdaArgument.builderInferenceSession = this }
            } else {
                null
            }

        val temporaryTrace = if (builderInferenceSession != null)
            TemporaryBindingTrace.create(trace, "Trace to resolve builder inference lambda: $lambdaArgument")
        else
            null

        (temporaryTrace ?: trace).record(
            BindingContext.NEW_INFERENCE_LAMBDA_INFO,
            psiCallArgument.cjFunction,
            lambdaInfo
        )

        val actualContext = outerCallContext
            .replaceBindingTrace(temporaryTrace ?: trace)
            .replaceContextDependency(lambdaInfo.contextDependency)
            .replaceExpectedType(approximatesExpectedType)
            .replaceDataFlowInfo(psiCallArgument.dataFlowInfoBeforeThisArgument).let {
                if (builderInferenceSession != null) it.replaceInferenceSession(builderInferenceSession) else it
            }

        val functionTypeInfo = expressionTypingServices.getTypeInfo(psiCallArgument.expression, actualContext)
        (temporaryTrace ?: trace).record(
            BindingContext.NEW_INFERENCE_LAMBDA_INFO,
            psiCallArgument.cjFunction,
            LambdaInfo.STUB_EMPTY
        )

        if (builderInferenceSession?.hasInapplicableCall() == true) {
            return ReturnArgumentsAnalysisResult(
                ReturnArgumentsInfo.empty, builderInferenceSession, hasInapplicableCallForBuilderInference = true
            )
        } else {
            temporaryTrace?.commit()
        }

        var hasReturnWithoutExpression = false
        var returnArgumentFound = false
        val returnArguments = lambdaInfo.returnStatements.mapNotNullTo(ArrayList()) { (expression, contextInfo) ->
            returnArgumentFound = true
            val returnedExpression = expression.returnedExpression
            if (returnedExpression != null) {
                createCallArgument(
                    returnedExpression,
                    contextInfo?.typeInfo
                        ?: throw AssertionError("typeInfo should be non-null for return with expression"),
                    contextInfo.lexicalScope,
                    contextInfo.trace
                )
            } else {
                hasReturnWithoutExpression = true
                EmptyLabeledReturn(expression, builtIns)
            }
        }

        val lastExpressionArgument = getLastDeparentesizedExpression(psiCallArgument)?.let { lastExpression ->
            if (lambdaInfo.returnStatements.any { (expression, _) -> expression == lastExpression }) {
                return@let null
            }

            val lastExpressionType = trace.getType(lastExpression)
            val contextInfo = lambdaInfo.lastExpressionInfo
            val lastExpressionTypeInfo =
                CangJieTypeInfo(lastExpressionType, contextInfo.dataFlowInfoAfter ?: functionTypeInfo.dataFlowInfo)
            createCallArgument(lastExpression, lastExpressionTypeInfo, contextInfo.lexicalScope, contextInfo.trace)
        }

        val lastExpressionCoercedToUnit = expectedReturnType?.isUnit() == true || hasReturnWithoutExpression
        if (!lastExpressionCoercedToUnit && lastExpressionArgument != null) {
            returnArgumentFound = true
            returnArguments += lastExpressionArgument
        }

        return ReturnArgumentsAnalysisResult(
            ReturnArgumentsInfo(
                returnArguments,
                lastExpressionArgument,
                lastExpressionCoercedToUnit,
                returnArgumentFound
            ),
            builderInferenceSession,
        )
    }

    private fun getLastDeparentesizedExpression(psiCallArgument: PSICangJieCallArgument): CjExpression? {
        val lastExpression = if (psiCallArgument is LambdaCangJieCallArgumentImpl) {
            psiCallArgument.cjLambdaExpression.bodyExpression?.statements?.lastOrNull()
        } else {
            (psiCallArgument as FunctionExpressionImpl).cjFunction.bodyExpression?.lastBlockStatementOrThis()
        }

        return CjPsiUtil.deparenthesize(lastExpression)
    }

    override fun getCandidateFactoryForInvoke(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall
    ): PSICallResolver.FactoryProviderForInvoke =
        psiCallResolver.FactoryProviderForInvoke(topLevelCallContext, scopeTower, cangjieCall as PSICangJieCallImpl)

    override fun findResultType(
        constraintSystem: NewConstraintSystem,
        typeVariable: TypeVariableTypeConstructor
    ): CangJieType? {
        val variableWithConstraints =
            constraintSystem.getBuilder().currentStorage().notFixedTypeVariables[typeVariable] ?: return null
        return resultTypeResolver.findResultType(
            constraintSystem.asConstraintSystemCompleterContext(),
            variableWithConstraints,
            TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
        ) as CangJieType
    }

    override fun createEmptyConstraintSystem(): NewConstraintSystem = NewConstraintSystemImpl(
        callComponents.constraintInjector,
        callComponents.builtIns,
        callComponents.cangjieTypeRefiner,
        callComponents.languageVersionSettings
    )

    override fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom) {
        cangjieToResolvedCallTransformer.createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(
            candidate, trace, emptyList(), substitutor = null
        )
    }

    override fun convertSignedConstantToUnsigned(argument: CangJieCallArgument): IntegerValueTypeConstant? {
        val argumentExpression = argument.psiExpression ?: return null
        return convertSignedConstantToUnsigned(argumentExpression)
    }

    private fun constantCanBeConvertedToUnsigned(constant: CompileTimeConstant<*>): Boolean {
        return !constant.isError && constant.parameters.isPure
    }

    private fun convertSignedConstantToUnsigned(expression: CjExpression): IntegerValueTypeConstant? {
        val constant = trace[BindingContext.COMPILE_TIME_VALUE, expression]
        if (constant !is IntegerValueTypeConstant || !constantCanBeConvertedToUnsigned(constant)) return null

        return with(IntegerValueTypeConstant) {
            constant.convertToUnsignedConstant(moduleDescriptor)
        }
    }
//    override fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean {
//        TODO("Not yet implemented")
//    }

    override fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType? {
        val candidateDescriptor = resolvedAtom.candidateDescriptor as? FunctionDescriptor ?: return null
        val call = (resolvedAtom.atom as? PSICangJieCall)?.psiCall ?: return null

        if (call.typeArgumentList != null || !candidateDescriptor.isFunctionForExpectTypeFromCastFeature()) return null
        val binaryParent = call.calleeExpression?.getBinaryWithTypeParent() ?: return null
        val operationType = binaryParent.operationReference.getReferencedNameElementType().takeIf {
            it == CjTokens.AS_KEYWORD
        } ?: return null

        val leftType = trace.get(BindingContext.TYPE, binaryParent.right ?: return null) ?: return null
        val expectedType = /*if (operationType == CjTokens.AS_SAFE) leftType.makeOptional() else*/ leftType
        val resultType = expectedType.unwrap()
        trace.record(BindingContext.CAST_TYPE_USED_AS_EXPECTED_TYPE, binaryParent)
        return resultType
    }

    override fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom) {
//        val atom = resolvedAtom.atom as? PSICangJieCall ?: return
//        disableContractsInsideContractsBlock(atom.psiCall, resolvedAtom.candidateDescriptor, topLevelCallContext.scope, trace)

    }

    override fun getLhsResult(call: CangJieCall): LHSResult {
        return if (call.explicitReceiver?.receiver != null) {

            return ((call.explicitReceiver?.receiver as? QualifierReceiver)?.classValueReceiver?.type as? UnwrappedType)?.let {
                LHSResult.Type(
                    call.explicitReceiver?.receiver as? QualifierReceiver, it
                )
            } ?: LHSResult.Error
        } else {
            val callableReferenceExpression = call.psiCangJieCall.psiCall.calleeExpression as? CjCallableReference
                ?: throw IllegalStateException("Not a callable reference")
            val lhsResult = psiCallResolver.getLhsResult(topLevelCallContext, callableReferenceExpression)
            return lhsResult
        }


    }
}
