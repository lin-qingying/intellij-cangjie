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

package cn.cangnova.cangjie.resolve.calls.inference

import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.config.LanguageFeature
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import cn.cangnova.cangjie.descriptors.impl.LocalVariableDescriptor
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.CjLambdaExpression
import cn.cangnova.cangjie.psi.CjVariableDeclaration
import cn.cangnova.cangjie.psi.psiUtil.anyDescendantOfType
import cn.cangnova.cangjie.resolve.DoubleColonExpressionResolver
import cn.cangnova.cangjie.resolve.MissingSupertypesResolver
import cn.cangnova.cangjie.resolve.TemporaryBindingTrace
import cn.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import cn.cangnova.cangjie.resolve.calls.components.InferenceSession
import cn.cangnova.cangjie.resolve.calls.components.NewConstraintSystemImpl
import cn.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzer
import cn.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import cn.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import cn.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import cn.cangnova.cangjie.resolve.calls.inference.model.*
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.resolve.calls.tower.*
import cn.cangnova.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import cn.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.checker.NewCapturedType
import cn.cangnova.cangjie.types.expressions.ExpressionTypingServices
import cn.cangnova.cangjie.types.util.asTypeProjection
import cn.cangnova.cangjie.types.util.contains
import cn.cangnova.cangjie.types.util.shouldBeUpdated

class BuilderInferenceSession(
    psiCallResolver: PSICallResolver,
    postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    callComponents: CangJieCallComponents,
    builtIns: CangJieBuiltIns,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>,
    private val trace: BindingTrace,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val expressionTypingServices: ExpressionTypingServices,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val lambdaArgument: LambdaCangJieCallArgument
) : StubTypesBasedInferenceSession<CallableDescriptor>(
    psiCallResolver, postponedArgumentsAnalyzer, cangjieConstraintSystemCompleter, callComponents, builtIns
) {
    private val commonSystem = NewConstraintSystemImpl(
        callComponents.constraintInjector,
        builtIns,
        callComponents.cangjieTypeRefiner,
        topLevelCallContext.languageVersionSettings
    )
    private var hasInapplicableCall = false
    private val commonCalls = arrayListOf<PSICompletedCallInfo>()
    private lateinit var lambda: ResolvedLambdaAtom

    override val parentSession: InferenceSession = topLevelCallContext.inferenceSession
    private val commonExpressions = arrayListOf<CjExpression>()


    private fun isTopLevelBuilderInferenceCall() = findParentBuildInferenceSession() == null
    fun addExpression(expression: CjExpression) {
        commonExpressions.add(expression)
    }
    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        constraintSystemBuilder: ConstraintSystemBuilder,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType>? {
        initializeLambda(lambda)

        val initialStorage = constraintSystemBuilder.currentStorage()
        val resultingSubstitutor by lazy {
            val storageSubstitutor =
                initialStorage.buildResultingSubstitutor(commonSystem, transformTypeVariablesToErrorTypes = false)
            ComposedSubstitutor(storageSubstitutor, commonSystem.buildCurrentSubstitutor() as NewTypeSubstitutor)
        }

        val effectivelyEmptyConstraintSystem = initializeCommonSystem(initialStorage)

        if (effectivelyEmptyConstraintSystem) {
            if (isTopLevelBuilderInferenceCall()) {
                updateAllCalls(resultingSubstitutor)
            }
            return null
        }

        cangjieConstraintSystemCompleter.completeConstraintSystem(
            commonSystem.asConstraintSystemCompleterContext(),
            builtIns.unitType,
            commonPartiallyResolvedCalls.map { it.callResolutionResult.resultCallAtom },
            completionMode,
            diagnosticsHolder
        )

        if (completionMode == ConstraintSystemCompletionMode.FULL) {
            constraintSystemBuilder.substituteFixedVariables(
                ComposedSubstitutor(resultingSubstitutor, createNonFixedTypeToVariableSubstitutor())
            )
        }

        if (isTopLevelBuilderInferenceCall()) {
            updateAllCalls(resultingSubstitutor)
        }

        @Suppress("UNCHECKED_CAST")
        return commonSystem.fixedTypeVariables as Map<TypeConstructor, UnwrappedType> // TODO
    }

    private fun getNestedBuilderInferenceSessions() = buildList {
        for (nestedSession in nestedInferenceSessions) {
            when (nestedSession) {
                is BuilderInferenceSession -> add(nestedSession)
//                is DelegateInferenceSession -> addAll(nestedSession.getNestedBuilderInferenceSessions())
            }
        }
    }

    private fun findAllParentBuildInferenceSessions() = buildList {
        var currentSession: BuilderInferenceSession? = findParentBuildInferenceSession()

        while (currentSession != null) {
            add(currentSession)
            currentSession = currentSession.findParentBuildInferenceSession()
        }
    }

    private fun extractCommonCapturedTypes(a: CangJieType, b: CangJieType): List<NewCapturedType> {
        val extractedCapturedTypes = mutableSetOf<NewCapturedType>().also { extractCapturedTypesTo(a, it) }
        return extractedCapturedTypes.filter { capturedType -> b.contains { it.constructor === capturedType.constructor } }
    }

    private fun extractCapturedTypesTo(type: CangJieType, to: MutableSet<NewCapturedType>) {
        if (type is NewCapturedType) {
            to.add(type)
        }
        for (typeArgument in type.arguments) {

            extractCapturedTypesTo(typeArgument.type, to)
        }
    }

    private fun substituteNotFixedVariables(
        lowerType: CangJieType,
        upperType: CangJieType,
        nonFixedToVariablesSubstitutor: NewTypeSubstitutor
    ): Pair<CangJieType, CangJieType> {
        val commonCapTypes = extractCommonCapturedTypes(lowerType, upperType)
        val substitutedCommonCapType = commonCapTypes.associate {
            it.constructor as TypeConstructor to nonFixedToVariablesSubstitutor.safeSubstitute(it).asTypeProjection()
        }

        val capTypesSubstitutor =
            TypeConstructorSubstitution.createByConstructorsMap(substitutedCommonCapType).buildSubstitutor()

        val substitutedLowerType =
            nonFixedToVariablesSubstitutor.safeSubstitute(capTypesSubstitutor.substitute(lowerType.unwrap()))
        val substitutedUpperType =
            nonFixedToVariablesSubstitutor.safeSubstitute(capTypesSubstitutor.substitute(upperType.unwrap()))

        return substitutedLowerType to substitutedUpperType
    }

    private fun integrateConstraints(
        storage: ConstraintStorage,
        nonFixedToVariablesSubstitutor: NewTypeSubstitutor,
        shouldIntegrateAllConstraints: Boolean
    ) {
        storage.notFixedTypeVariables.values.forEach {
            commonSystem.registerTypeVariableIfNotPresent(it.typeVariable)
        }

        /*
        * storage can contain the following substitutions:
        *  TypeVariable(A) -> ProperType
        *  TypeVariable(B) -> Special-Non-Fixed-Type
        *
        * while substitutor from parameter map non-fixed types to the original type variable
        * */
        val callSubstitutor =
            storage.buildResultingSubstitutor(commonSystem, transformTypeVariablesToErrorTypes = false)

        for (initialConstraint in storage.initialConstraints) {
            if (initialConstraint.position is BuilderInferencePosition) continue

            val substitutedConstraint = initialConstraint.substitute(callSubstitutor)
            val (lower, upper) = substituteNotFixedVariables(
                substitutedConstraint.a as CangJieType,
                substitutedConstraint.b as CangJieType,
                nonFixedToVariablesSubstitutor
            )

            if (commonSystem.isProperType(lower) && commonSystem.isProperType(upper)) continue

            when (initialConstraint.constraintKind) {
                ConstraintKind.LOWER -> error("LOWER constraint shouldn't be used, please use UPPER")

                ConstraintKind.UPPER -> commonSystem.addSubtypeConstraint(lower, upper, substitutedConstraint.position)

                ConstraintKind.EQUALITY ->
                    with(commonSystem) {
                        addSubtypeConstraint(lower, upper, substitutedConstraint.position)
                        addSubtypeConstraint(upper, lower, substitutedConstraint.position)
                    }
            }
        }

        if (shouldIntegrateAllConstraints) {
            for ((variableConstructor, type) in storage.fixedTypeVariables) {
                val typeVariable = storage.allTypeVariables.getValue(variableConstructor)
                commonSystem.registerTypeVariableIfNotPresent(typeVariable)
                commonSystem.addEqualityConstraint(
                    (typeVariable as NewTypeVariable).defaultType,
                    type,
                    BuilderInferencePosition
                )
            }
        }
    }


    private fun InitialConstraint.substitute(substitutor: NewTypeSubstitutor): InitialConstraint {
        val lowerSubstituted = substitutor.safeSubstitute(a as UnwrappedType)
        val upperSubstituted = substitutor.safeSubstitute(b as UnwrappedType)

        if (lowerSubstituted == a && upperSubstituted == b) return this

//        val isInferringIntoUpperBoundsForbidden = expressionTypingServices.languageVersionSettings.supportsFeature(
//            LanguageFeature.ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
//        )
        val isFromNotSubstitutedDeclaredUpperBound = upperSubstituted == b && position is DeclaredUpperBoundConstraintPosition<*>

        val resultingPosition = if (isFromNotSubstitutedDeclaredUpperBound/* && isInferringIntoUpperBoundsForbidden*/) {
            position
        } else {
            BuilderInferenceSubstitutionConstraintPositionImpl(lambdaArgument, this, isFromNotSubstitutedDeclaredUpperBound)
        }

        return InitialConstraint(
            lowerSubstituted,
            upperSubstituted,
            constraintKind,
            resultingPosition
        )
    }

    private fun initializeCommonSystem(initialStorage: ConstraintStorage): Boolean {
        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()

        for (parentSession in findAllParentBuildInferenceSessions()) {
            for ((variable, stubType) in parentSession.stubsForPostponedVariables) {
                commonSystem.registerTypeVariableIfNotPresent(variable)
                commonSystem.addSubtypeConstraint(
                    variable.defaultType,
                    stubType,
                    InjectedAnotherStubTypeConstraintPositionImpl(lambdaArgument)
                )
            }
        }

        integrateConstraints(initialStorage, nonFixedToVariablesSubstitutor, false)

        for (call in commonCalls + commonPartiallyResolvedCalls) {
            val storage = call.callResolutionResult.constraintSystem.getBuilder().currentStorage()
            integrateConstraints(
                storage,
                nonFixedToVariablesSubstitutor,
                shouldIntegrateAllConstraints = call is PSIPartialCallInfo
            )
        }

        return commonSystem.notFixedTypeVariables.all { it.value.constraints.isEmpty() }
    }

    /*
     * We update calls in top-down way:
     * - updating calls within top-level builder inference call
     * - ...
     * - updating calls within the deepest builder inference call
     */
    private fun updateAllCalls(substitutor: NewTypeSubstitutor) {
        updateCalls(
            lambda,
            substitutor = substitutor,
            commonSystem.errors
        )

        val nestedBuilderInferenceSessions = getNestedBuilderInferenceSessions()

        for (nestedSession in nestedBuilderInferenceSessions) {
            // TODO: exclude injected variables
            nestedSession.updateAllCalls(
                ComposedSubstitutor(
                    nestedSession.commonSystem.buildCurrentSubstitutor() as NewTypeSubstitutor,
                    substitutor
                )
            )
        }
    }

    private fun skipCall(callInfo: SingleCallResolutionResult): Boolean {
        val descriptor = callInfo.resultCallAtom.candidateDescriptor

        // FakeCallableDescriptorForObject can't introduce new information for inference,
        // so it's safe to complete it fully
        return descriptor is FakeCallableDescriptorForObject
    }

    fun hasInapplicableCall(): Boolean = hasInapplicableCall

    private fun arePostponedVariablesInferred() = commonSystem.notFixedTypeVariables.isEmpty()

    override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean {
        return !skipCall(callInfo) && !arePostponedVariablesInferred()

    }

    override fun initializeLambda(lambda: ResolvedLambdaAtom) {
        this.lambda = lambda

    }

    private fun findParentBuildInferenceSession(): BuilderInferenceSession? {
        var currentSession: InferenceSession? = parentSession

        while (currentSession != null) {
            if (currentSession is BuilderInferenceSession) return currentSession
            currentSession = currentSession.parentSession
        }

        return null
    }

    private fun createNonFixedTypeToVariableSubstitutor() =
        NewTypeSubstitutorByConstructorMap(createNonFixedTypeToVariableMap())

    private fun createNonFixedTypeToVariableMap(): Map<TypeConstructor, UnwrappedType> {
        val bindings = hashMapOf<TypeConstructor, UnwrappedType>()

        for ((variable, nonFixedType) in stubsForPostponedVariables) { // do it for nested sessions
            bindings[nonFixedType.constructor] = variable.defaultType
        }

        val parentBuilderInferenceCallSession = findParentBuildInferenceSession()

        if (parentBuilderInferenceCallSession != null) {
            bindings.putAll(parentBuilderInferenceCallSession.createNonFixedTypeToVariableMap())
        }

        return bindings
    }

    /*
  * It's used only for `+=` resolve to clear calls info before the second analysis of right side.
  * TODO: remove it after moving `+=` resolve into OR mechanism
  */
    fun clearCallsInfoByContainingElement(containingElement: CjElement) {
        commonCalls.removeIf remove@{ callInfo ->
            val atom = callInfo.callResolutionResult.resultCallAtom.atom
            if (atom !is PSICangJieCallImpl) return@remove false

            containingElement.anyDescendantOfType<CjElement> { it == atom.psiCall.callElement }
        }
    }

    private fun createResolvedAtomCompleter(
        resultSubstitutor: NewTypeSubstitutor,
        context: BasicCallResolutionContext
    ): ResolvedAtomCompleter {
        return ResolvedAtomCompleter(
            resultSubstitutor, context, cangjieToResolvedCallTransformer,
            expressionTypingServices, argumentTypeResolver, doubleColonExpressionResolver,  builtIns,
            deprecationResolver, moduleDescriptor, context.dataFlowValueFactory, typeApproximator, missingSupertypesResolver,
            callComponents,
        )
    }
    private fun findTopLevelTrace(): BindingTrace {
        var currentSession = this
        while (true) {
            currentSession = currentSession.findParentBuildInferenceSession() ?: break
        }
        return currentSession.topLevelCallContext.trace
    }
    private fun updateCall(
        completedCall: PSICompletedCallInfo,
        nonFixedTypesToResultSubstitutor: NewTypeSubstitutor,
        nonFixedTypesToResult: Map<TypeConstructor, UnwrappedType>
    ) {
        val storage = completedCall.callResolutionResult.constraintSystem.getBuilder().currentStorage()
        val resultingCallSubstitutor = storage.fixedTypeVariables.entries
            .associate { it.key to nonFixedTypesToResultSubstitutor.safeSubstitute(it.value as UnwrappedType) } // TODO: SUB

        @Suppress("UNCHECKED_CAST")
        val resultingSubstitutor =
            NewTypeSubstitutorByConstructorMap((resultingCallSubstitutor + nonFixedTypesToResult) as Map<TypeConstructor, UnwrappedType>) // TODO: SUB

        val atomCompleter = createResolvedAtomCompleter(
            resultingSubstitutor,
            completedCall.context.replaceBindingTrace(findTopLevelTrace()).replaceInferenceSession(this)
        )

        completeCall(completedCall, atomCompleter)
    }
    private fun reportErrors(completedCall: CallInfo, resolvedCall: NewAbstractResolvedCall<*>, errors: List<ConstraintSystemError>) {
        cangjieToResolvedCallTransformer.reportCallDiagnostic(
            completedCall.context,
            trace,
            resolvedCall,
            resolvedCall.resultingDescriptor,
            errors.asDiagnostics()
        )
    }
    private fun completeCall(
        callInfo: CallInfo,
        atomCompleter: ResolvedAtomCompleter
    ): NewAbstractResolvedCall<*>? {
        val resultCallAtom = callInfo.callResolutionResult.resultCallAtom
        resultCallAtom.subResolvedAtoms?.forEach { subResolvedAtom ->
            atomCompleter.completeAll(subResolvedAtom)
        }
        val resolvedCall = atomCompleter.completeResolvedCall(resultCallAtom, callInfo.callResolutionResult.diagnostics)

        val callTrace = callInfo.context.trace
        if (callTrace is TemporaryBindingTrace) {
            callTrace.commit()
        }
        return resolvedCall
    }

    private fun updateExpressionDescriptorAndType(expression: CjExpression, substitutor: NewTypeSubstitutor) {
        val currentExpressionType = trace.getType(expression)
        if (currentExpressionType != null) {
            trace.recordType(expression, substitutor.safeSubstitute(currentExpressionType.unwrap()))
        }

        val (currentDescriptorType, updateDescriptorType) = when (expression) {
            is CjLambdaExpression -> {
                val descriptor = trace[BindingContext.FUNCTION, expression.functionLiteral] as? AnonymousFunctionDescriptor
                    ?: return
                val currentType = descriptor.returnType ?: return
                currentType to descriptor::setReturnType
            }
            is CjVariableDeclaration -> {
                val descriptor = trace[BindingContext.VARIABLE, expression] as? LocalVariableDescriptor ?: return
                descriptor.type to descriptor::setOutType
            }
//            is CjDoubleColonExpression -> {
//                completeDoubleColonExpression(expression, substitutor)
//                return
//            }
            else -> return
        }

        if (currentDescriptorType.shouldBeUpdated()) {
            updateDescriptorType(substitutor.safeSubstitute(currentDescriptorType.unwrap()))
        }
    }

    companion object {
        private fun BuilderInferenceSession.updateCalls(
            lambda: ResolvedLambdaAtom,
            substitutor: NewTypeSubstitutor,
            errors: List<ConstraintSystemError>
        ) {
            val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()

            val nonFixedTypesToResult =
                nonFixedToVariablesSubstitutor.map.mapValues { substitutor.safeSubstitute(it.value) }
            val nonFixedTypesToResultSubstitutor = ComposedSubstitutor(substitutor, nonFixedToVariablesSubstitutor)

            val atomCompleter = createResolvedAtomCompleter(
                nonFixedTypesToResultSubstitutor,
                topLevelCallContext.replaceBindingTrace(findTopLevelTrace()).replaceInferenceSession(this)
            )

            for (expression in commonExpressions) {
                updateExpressionDescriptorAndType(expression, nonFixedTypesToResultSubstitutor)
            }

            for (call in commonCalls) {
                updateCall(call, nonFixedTypesToResultSubstitutor, nonFixedTypesToResult)
                reportErrors(call, call.resolvedCall, errors)
            }

            for (call in commonPartiallyResolvedCalls) {
                val resolvedCall = completeCall(call, atomCompleter) ?: continue
                reportErrors(call, resolvedCall, errors)
            }

            atomCompleter.completeAll(lambda)
        }
    }
}
class ComposedSubstitutor(val left: NewTypeSubstitutor, val right: NewTypeSubstitutor) : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
        val rightSubstitution = right.substituteNotNullTypeWithConstructor(constructor)
        return left.substituteNotNullTypeWithConstructor(rightSubstitution?.constructor ?: constructor) ?: rightSubstitution
    }

    override val isEmpty: Boolean get() = left.isEmpty && right.isEmpty
}
