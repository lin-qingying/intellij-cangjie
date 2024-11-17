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

package com.linqingying.cangjie.resolve.calls.components

import com.intellij.util.SmartList
import com.linqingying.cangjie.builtins.UnsignedTypes
import com.linqingying.cangjie.builtins.getReceiverTypeFromFunctionType
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.DescriptorVisibilities.PRIVATE
import com.linqingying.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.linqingying.cangjie.descriptors.enumd.LazyEnumDescriptor
import com.linqingying.cangjie.descriptors.impl.CallableDescriptorForExtend
import com.linqingying.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjCallExpression
import com.linqingying.cangjie.psi.CjNameReferenceExpression
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.linqingying.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.linqingying.cangjie.resolve.calls.inference.*
import com.linqingying.cangjie.resolve.calls.inference.components.*
import com.linqingying.cangjie.resolve.calls.inference.model.*
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.linqingying.cangjie.resolve.calls.tower.*
import com.linqingying.cangjie.resolve.calls.util.getReceiverValueWithSmartCast
import com.linqingying.cangjie.resolve.isExtension
import com.linqingying.cangjie.resolve.isInsideInterface
import com.linqingying.cangjie.resolve.isStatic
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.receivers.ClassQualifier
import com.linqingying.cangjie.resolve.scopes.receivers.ClassValueReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.EnumClassQualifier
import com.linqingying.cangjie.resolve.scopes.receivers.TypeAliasQualifier
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.model.CangJieTypeMarker
import com.linqingying.cangjie.types.model.TypeConstructorMarker
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.types.util.contains
import com.linqingying.cangjie.types.util.makeNotNullable
import com.linqingying.cangjie.types.util.makeOptional
import com.linqingying.cangjie.utils.OperatorNameConventions
import com.linqingying.cangjie.utils.compactIfPossible

/**
 * 检查通过call调用操作符函数的情况
 */
internal object CheckOperatorCallPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        if (candidateDescriptor !is FunctionDescriptor) return
//禁止使用call方式调用操作符函数
        if ((candidateDescriptor as FunctionDescriptor).isOperator && resolvedCall.atom.psiCangJieCall.psiCall.callElement is CjCallExpression) {
            val funcName = candidateDescriptor.name
            if (funcName == OperatorNameConventions.INVOKE
                || funcName == OperatorNameConventions.GET
            ) return

            addDiagnostic(NoCallOperatorFunction(candidateDescriptor))

        }
    }
}

internal object CheckSuperExpressionCallPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor

        if (callComponents.statelessCallbacks.isSuperExpression(resolvedCall.dispatchReceiverArgument)) {
            if (candidateDescriptor is CallableMemberDescriptor) {
                checkSuperCandidateDescriptor(candidateDescriptor)
            }
        }

        val extensionReceiver = resolvedCall.extensionReceiverArgument
        if (extensionReceiver != null && callComponents.statelessCallbacks.isSuperExpression(extensionReceiver)) {
            addDiagnostic(SuperAsExtensionReceiver(extensionReceiver))
        }
    }

    private fun ResolutionCandidate.checkSuperCandidateDescriptor(candidateDescriptor: CallableMemberDescriptor) {
        if (candidateDescriptor.modality == Modality.ABSTRACT) {
            addDiagnostic(AbstractSuperCall(resolvedCall.dispatchReceiverArgument!!))
        } else if (candidateDescriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            var intersectionFakeOverrideDescriptor = candidateDescriptor
            while (intersectionFakeOverrideDescriptor.overriddenDescriptors.size == 1) {
                intersectionFakeOverrideDescriptor = intersectionFakeOverrideDescriptor.overriddenDescriptors.first()
                if (intersectionFakeOverrideDescriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                    return
                }
            }
            if (intersectionFakeOverrideDescriptor.overriddenDescriptors.size > 1) {
                if (intersectionFakeOverrideDescriptor.overriddenDescriptors.firstOrNull {
                        !it.isInsideInterface
                    }?.modality == Modality.ABSTRACT
                ) {
                    addDiagnostic(AbstractFakeOverrideSuperCall)
                }
            }
        }
    }
}

internal object CheckEnumCall : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val descriptor = resolvedCall.candidateDescriptor as? EnumClassCallableDescriptor ?: return


        val callExpression = resolvedCall.atom.psiCangJieCall.psiCall.callElement
        if (DescriptorUtils.isEnum(descriptor) && callExpression is CjCallExpression && callExpression.typeArgumentList != null) {
            addDiagnostic(EmptyDiagnostic)
            return
        }
        val isCall = callExpression is CjCallExpression && callExpression.typeArgumentList != null
        if (DescriptorUtils.isEnumEntry(descriptor)) {
            if (descriptor.hashUnsubstitutedPrimaryConstructor() && isCall) {
                addDiagnostic(EmptyDiagnostic)
            } else if (!descriptor.hashUnsubstitutedPrimaryConstructor() && !isCall) {
                addDiagnostic(EmptyDiagnostic)
            }
        }


    }
}

fun LexicalScope.isStaticContext(): Boolean {
    return ownerDescriptor.isStatic
}

fun ResolutionCandidate.isStaticContext(): Boolean {
    //    调用上下文的Scope是否输入静态声明
    fun isStaticContext(): Boolean {
        return scopeTower.lexicalScope.isStaticContext()
    }


    resolvedCall.atom.explicitReceiver ?: return isStaticContext()
    return when (val value = resolvedCall.atom.explicitReceiver!!.receiver) {

        is EnumClassQualifier -> {
            value.descriptor.kind == ClassKind.ENUM
//                !(value.descriptor.kind == ClassKind.ENUM || value.descriptor.kind == ClassKind.ENUM_ENTRY)

        }

        is TypeAliasQualifier -> {
            true
        }

        is ClassQualifier -> {
            !(value.descriptor.kind == ClassKind.ENUM || value.descriptor.kind == ClassKind.ENUM_ENTRY)

        }

        else -> false
    }


}


internal object CheckStaticCall : ResolutionPart() {


    override fun ResolutionCandidate.process(workIndex: Int) {
        val descriptor = this.descriptor


//        是否为static上下文
        val isStaticContext = isStaticContext()

        val kind = descriptor.getDescriptorKind()
        val memberStatic = descriptor.isStatic()
//        非静态上下文访问静态成员
        if (memberStatic && (!isStaticContext && (resolvedCall.explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER || resolvedCall.explicitReceiverKind == ExplicitReceiverKind.EXTENSION_RECEIVER))) {
            addDiagnostic(NonStaticContextAccessStaticMemberDiagnostic(kind, descriptor))
        }

//静态上下文访问非静成员
        if (!memberStatic && isStaticContext && !descriptor.isLocal && !descriptor.isTopLevel

        ) {
            addDiagnostic(StaticContextAccessNonStaticMemberDiagnostic(kind, descriptor))
        }


    }

}

private data class ApplicableContextReceiverArgumentWithConstraint(
    val argument: SimpleCangJieCallArgument,
    val argumentType: UnwrappedType,
    val expectedType: UnwrappedType,
    val position: ConstraintPosition
)

private fun ResolutionCandidate.getReceiverArgumentWithConstraintIfCompatible(
    argument: SimpleCangJieCallArgument,
    parameter: ParameterDescriptor
): ApplicableContextReceiverArgumentWithConstraint? {
    val csBuilder = getSystem().getBuilder()
    val expectedTypeUnprepared = argument.getExpectedType(parameter, callComponents.languageVersionSettings)
    val expectedType = prepareExpectedType(expectedTypeUnprepared)
    val argumentType = captureFromTypeParameterUpperBoundIfNeeded(argument.receiver.stableType, expectedType)
    val position = ReceiverConstraintPositionImpl(argument, resolvedCall.atom)
    return if (csBuilder.isSubtypeConstraintCompatible(argumentType, expectedType, position))
        ApplicableContextReceiverArgumentWithConstraint(argument, argumentType, expectedType, position)
    else null
}

internal enum class ImplicitInvokeCheckStatus {
    NO_INVOKE, INVOKE_ON_NOT_NULL_VARIABLE, UNSAFE_INVOKE_REPORTED
}

//检查扩展接收器
internal object CheckReceivers : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        when (workIndex) {
            0 -> checkReceiver(
                resolvedCall.dispatchReceiverArgument,
                candidateDescriptor.dispatchReceiverParameter,
                shouldCheckImplicitInvoke = true,
            )

            1 -> {
                var extensionReceiverArgument = resolvedCall.extensionReceiverArgument
                if (extensionReceiverArgument == null) {
                    extensionReceiverArgument = chooseExtensionReceiverCandidate() ?: return
                    resolvedCall.extensionReceiverArgument = extensionReceiverArgument
                }
                val checkBuilderInferenceRestriction =
                    !callComponents.languageVersionSettings
                        .supportsFeature(LanguageFeature.NoBuilderInferenceWithoutAnnotationRestriction)
                if (checkBuilderInferenceRestriction &&
                    extensionReceiverArgument.receiver.receiverValue.type is StubTypeForBuilderInference
                ) {
                    addDiagnostic(
                        StubBuilderInferenceReceiver(
                            extensionReceiverArgument,
                            candidateDescriptor.extensionReceiverParameter!!
                        )
                    )
                }
                checkReceiver(
                    resolvedCall.extensionReceiverArgument,
                    candidateDescriptor.extensionReceiverParameter,
                    shouldCheckImplicitInvoke = false, // reproduce old inference behaviour
                )
            }
        }
    }

    override fun ResolutionCandidate.workCount() = 2

    private fun ResolutionCandidate.chooseExtensionReceiverCandidate(): SimpleCangJieCallArgument? {
        val receiverCandidates = resolvedCall.extensionReceiverArgumentCandidates
        if (receiverCandidates.isNullOrEmpty()) {
            return null
        }
        if (receiverCandidates.size == 1) {
            return receiverCandidates.single()
        }
        val extensionReceiverParameter = candidateDescriptor.extensionReceiverParameter ?: return null
        val compatible = receiverCandidates.mapNotNull {
            getReceiverArgumentWithConstraintIfCompatible(
                it,
                extensionReceiverParameter
            )
        }
        return when (compatible.size) {
            0 -> {
                addDiagnostic(NoMatchingContextReceiver())
                null
            }

            1 -> compatible.single().argument
            else -> {
                addDiagnostic(ContextReceiverAmbiguity())
                null
            }
        }
    }

    private fun ResolutionCandidate.checkReceiver(
        receiverArgument: SimpleCangJieCallArgument?,
        receiverParameter: ReceiverParameterDescriptor?,
        shouldCheckImplicitInvoke: Boolean,
    ) {
//        TODO 检查接收器会在使用操作符函数报错，所以注释了
//        if (this !is CallableReferenceResolutionCandidate && (receiverArgument == null) != (receiverParameter == null)) {
//            error("Inconsistency receiver state for call $cangjieCall and candidate descriptor: $candidateDescriptor")
//        }
        if (receiverArgument == null || receiverParameter == null) return

        val implicitInvokeState = if (shouldCheckImplicitInvoke) {
            checkUnsafeImplicitInvokeAfterSafeCall(receiverArgument)
        } else ImplicitInvokeCheckStatus.NO_INVOKE

        val receiverInfo = ReceiverInfo(
            isReceiver = true,
            shouldReportUnsafeCall = implicitInvokeState != ImplicitInvokeCheckStatus.UNSAFE_INVOKE_REPORTED,
            reportUnsafeCallAsUnsafeImplicitInvoke = implicitInvokeState == ImplicitInvokeCheckStatus.INVOKE_ON_NOT_NULL_VARIABLE,
            selectorCall = resolvedCall.atom
        )

        resolveCangJieArgument(receiverArgument, receiverParameter, receiverInfo)
    }
}

private fun ResolutionCandidate.checkUnsafeImplicitInvokeAfterSafeCall(argument: SimpleCangJieCallArgument): ImplicitInvokeCheckStatus {
    val variableForInvoke = variableCandidateIfInvoke ?: return ImplicitInvokeCheckStatus.NO_INVOKE

    val receiverArgument = with(variableForInvoke.resolvedCall) {
        when (explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiverArgument
            ExplicitReceiverKind.EXTENSION_RECEIVER,
            ExplicitReceiverKind.BOTH_RECEIVERS -> extensionReceiverArgument

            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> return ImplicitInvokeCheckStatus.INVOKE_ON_NOT_NULL_VARIABLE
        }
    } ?: error("Receiver kind does not match receiver argument")

    if (receiverArgument.isSafeCall && receiverArgument.receiver.stableType.isNullable() && resolvedCall.candidateDescriptor.typeParameters.isEmpty()) {
        addDiagnostic(UnsafeCallError(argument, isForImplicitInvoke = true))
        return ImplicitInvokeCheckStatus.UNSAFE_INVOKE_REPORTED
    }

    return ImplicitInvokeCheckStatus.INVOKE_ON_NOT_NULL_VARIABLE
}

//检查扩展之间的private修饰符访问
internal object CheckExtensionPrivateVisibility : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val containingDescriptor = scopeTower.lexicalScope.ownerDescriptor  //调用所在声明

        val dispatchReceiverArgument = resolvedCall.dispatchReceiverArgument
        resolvedCall.extensionReceiverArgument ?: return

        val callCandidateDescriptor = resolvedCall.candidateDescriptor //被调用声明
        val receiverValue =
            dispatchReceiverArgument?.receiver?.receiverValue ?: DescriptorVisibilities.ALWAYS_SUITABLE_RECEIVER
        val invisibleMember =
            DescriptorVisibilityUtils.findInvisibleMember(
                receiverValue,
                callCandidateDescriptor,
                containingDescriptor,
                callComponents.languageVersionSettings
            )

        if (dispatchReceiverArgument is ExpressionCangJieCallArgument) {
            val smartCastReceiver =
                getReceiverValueWithSmartCast(receiverValue, dispatchReceiverArgument.receiver.stableType)
            if (DescriptorVisibilityUtils.findInvisibleMember(
                    smartCastReceiver,
                    candidateDescriptor,
                    containingDescriptor,
                    callComponents.languageVersionSettings
                ) == null
            ) {
                addDiagnostic(
                    SmartCastDiagnostic(
                        dispatchReceiverArgument,
                        dispatchReceiverArgument.receiver.stableType,
                        resolvedCall.atom
                    )
                )
                return
            }
        }

        if (containingDescriptor is CallableDescriptor) {
            if (callCandidateDescriptor.extensionReceiverParameter?.value?.type?.let {
                    containingDescriptor.extensionReceiverParameter?.value?.type?.let { it1 ->
                        CangJieTypeChecker.DEFAULT.equalTypes(
                            it, it1
                        )
                    }
                } == true) {
//                判断扩展id是否相同
                if (invisibleMember == null && callCandidateDescriptor.visibility == PRIVATE) {
                    addDiagnostic(VisibilityError(callCandidateDescriptor))

                }
            }

        }


        if (invisibleMember is DeclarationDescriptorWithVisibility) {
            addDiagnostic(VisibilityError(invisibleMember))
        }


    }

}
internal object EagerResolveOfCallableReferences : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        getSubResolvedAtoms()
            .filterIsInstance<EagerCallableReferenceAtom>()
            .forEach {
                callComponents.callableReferenceArgumentResolver.processCallableReferenceArgument(
                    getSystem().getBuilder(), it, this, resolutionCallbacks
                )
            }
    }
}

internal object CheckVisibility : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val containingDescriptor = scopeTower.lexicalScope.ownerDescriptor
        val dispatchReceiverArgument = resolvedCall.dispatchReceiverArgument

        val receiverValue =
            dispatchReceiverArgument?.receiver?.receiverValue ?: DescriptorVisibilities.ALWAYS_SUITABLE_RECEIVER
        val invisibleMember =
            DescriptorVisibilityUtils.findInvisibleMember(
                receiverValue,
                resolvedCall.candidateDescriptor,
                containingDescriptor,
                callComponents.languageVersionSettings
            ) ?: return

        if (dispatchReceiverArgument is ExpressionCangJieCallArgument) {
            val smartCastReceiver =
                getReceiverValueWithSmartCast(receiverValue, dispatchReceiverArgument.receiver.stableType)
            if (DescriptorVisibilityUtils.findInvisibleMember(
                    smartCastReceiver,
                    candidateDescriptor,
                    containingDescriptor,
                    callComponents.languageVersionSettings
                ) == null
            ) {
                addDiagnostic(
                    SmartCastDiagnostic(
                        dispatchReceiverArgument,
                        dispatchReceiverArgument.receiver.stableType,
                        resolvedCall.atom
                    )
                )
                return
            }
        }


        if (invisibleMember is DeclarationDescriptorWithVisibility) {
            addDiagnostic(VisibilityError(invisibleMember))

        }
    }
}


internal object NoTypeArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(cangjieCall.typeArguments.isEmpty()) {
            "Variable call cannot has explicit type arguments: ${cangjieCall.typeArguments}. Call: $cangjieCall"
        }
        resolvedCall.typeArgumentMappingByOriginal =
            TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
    }
}


internal object PostponedVariablesInitializerResolutionPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val csBuilder = getSystem().getBuilder()
        for ((argument, parameter) in resolvedCall.argumentToCandidateParameter) {
            if (!callComponents.statelessCallbacks.isBuilderInferenceCall(argument, parameter)) continue
            val receiverType = parameter.type.getReceiverTypeFromFunctionType() ?: continue
//            val dontUseBuilderInferenceIfPossible =
//                callComponents.languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceOnlyIfNeeded)

            if (argument is LambdaCangJieCallArgument && !argument.hasBuilderInferenceAnnotation) {
                argument.hasBuilderInferenceAnnotation = true
            }

//            if (dontUseBuilderInferenceIfPossible) continue

            for (freshVariable in resolvedCall.freshVariablesSubstitutor.freshVariables) {
                if (resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(freshVariable.originalTypeParameter) is SimpleTypeArgument)
                    continue

                if (csBuilder.isPostponedTypeVariable(freshVariable)) continue
                if (receiverType.contains { it.constructor == freshVariable.originalTypeParameter.typeConstructor }) {
                    csBuilder.markPostponedVariable(freshVariable)
                }
            }
        }
    }
}

internal object MapTypeArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        resolvedCall.typeArgumentMappingByOriginal =
            callComponents.typeArgumentsToParametersMapper.mapTypeArguments(
                cangjieCall,
                candidateDescriptor.original
            )
                .also {
                    it.diagnostics.forEach(this@process::addDiagnostic)
                }
    }
}

internal object CollectionTypeVariableUsagesInfo : ResolutionPart() {
    private val CangJieType.isComputed get() = this !is WrappedType || isComputed()

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositions(
        variableTypeConstructor: TypeConstructorMarker,
        baseType: CangJieTypeMarker,
        wasOutVariance: Boolean = true
    ): Boolean {
        if (baseType !is CangJieType) return false

        val dependentTypeParameter = getTypeParameterByVariable(variableTypeConstructor) ?: return false
        val declaredTypeParameters = baseType.constructor.parameters

        if (declaredTypeParameters.size < baseType.arguments.size) return false

        for ((argumentsIndex, argument) in baseType.arguments.withIndex()) {
//            if ( argument.type.isMarkedOption) continue

            val currentEffectiveVariance = false
//                declaredTypeParameters[argumentsIndex].variance == Variance.OUT_VARIANCE || argument.projectionKind == Variance.OUT_VARIANCE
            val effectiveVarianceFromTopLevel = wasOutVariance && currentEffectiveVariance

            if ((argument.type.constructor == dependentTypeParameter || argument.type.constructor == variableTypeConstructor) && !effectiveVarianceFromTopLevel)
                return true

            if (isContainedInInvariantOrContravariantPositions(
                    variableTypeConstructor,
                    argument.type,
                    effectiveVarianceFromTopLevel
                )
            )
                return true
        }

        return false
    }

    private fun isContainedInInvariantOrContravariantPositionsAmongTypeParameters(
        checkingType: TypeVariableFromCallableDescriptor,
        typeParameters: List<TypeParameterDescriptor>
    ) = typeParameters.any {
        it.typeConstructor == checkingType.originalTypeParameter.typeConstructor
    }

    private fun NewConstraintSystem.getDependentTypeParameters(
        variable: TypeConstructorMarker,
        dependentTypeParametersSeen: List<Pair<TypeConstructorMarker, CangJieTypeMarker?>> = listOf()
    ): List<Pair<TypeConstructorMarker, CangJieTypeMarker?>> {
        val context = asConstraintSystemCompleterContext()
        val dependentTypeParameters = getBuilder().currentStorage().notFixedTypeVariables.asSequence()
            .flatMap { (typeConstructor, constraints) ->
                val upperBounds = constraints.constraints.filter {
                    it.position.from is DeclaredUpperBoundConstraintPositionImpl && it.kind == ConstraintKind.UPPER
                }

                upperBounds.mapNotNull { constraint ->
                    if (constraint.type.typeConstructor(context) != variable) {
                        val suitableUpperBound = upperBounds.find { upperBound ->
                            with(context) { upperBound.type.contains { it.typeConstructor() == variable } }
                        }?.type

                        if (suitableUpperBound != null) typeConstructor to suitableUpperBound else null
                    } else typeConstructor to null
                }
            }.filter { it !in dependentTypeParametersSeen && it.first != variable }.toList()

        return dependentTypeParameters + dependentTypeParameters.flatMapTo(SmartList()) { (typeConstructor, _) ->
            if (typeConstructor != variable) {
                getDependentTypeParameters(typeConstructor, dependentTypeParameters + dependentTypeParametersSeen)
            } else emptyList()
        }
    }

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositionsAmongUpperBound(
        checkingType: TypeConstructorMarker,
        dependentTypeParameters: List<Pair<TypeConstructorMarker, CangJieTypeMarker?>>
    ): Boolean {
        var currentTypeParameterConstructor = checkingType

        return dependentTypeParameters.any { (typeConstructor, upperBound) ->
            val isContainedOrNoUpperBound =
                upperBound == null || isContainedInInvariantOrContravariantPositions(
                    currentTypeParameterConstructor,
                    upperBound
                )
            currentTypeParameterConstructor = typeConstructor
            isContainedOrNoUpperBound
        }
    }

    private fun NewConstraintSystem.getTypeParameterByVariable(typeConstructor: TypeConstructorMarker) =
        (getBuilder().currentStorage().allTypeVariables[typeConstructor] as? TypeVariableFromCallableDescriptor)?.originalTypeParameter?.typeConstructor

    private fun NewConstraintSystem.getDependingOnTypeParameter(variable: TypeConstructor) =
        getBuilder().currentStorage().notFixedTypeVariables[variable]?.constraints?.mapNotNull {
            if (it.position.from is DeclaredUpperBoundConstraintPositionImpl && it.kind == ConstraintKind.UPPER) {
                it.type.typeConstructor(asConstraintSystemCompleterContext())
            } else null
        } ?: emptyList()

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositionsWithDependencies(
        variable: TypeVariableFromCallableDescriptor,
        declarationDescriptor: DeclarationDescriptor?
    ): Boolean {
        if (declarationDescriptor !is CallableDescriptor) return false

        val returnType = declarationDescriptor.returnType ?: return false

        if (!returnType.isComputed) return false

        val typeVariableConstructor = variable.freshTypeConstructor
        val dependentTypeParameters = getDependentTypeParameters(typeVariableConstructor)
        val dependingOnTypeParameter = getDependingOnTypeParameter(typeVariableConstructor)

        val isContainedInUpperBounds =
            isContainedInInvariantOrContravariantPositionsAmongUpperBound(
                typeVariableConstructor,
                dependentTypeParameters
            )
        val isContainedAnyDependentTypeInReturnType = dependentTypeParameters.any { (typeParameter, _) ->
            returnType.contains {
                it.typeConstructor(asConstraintSystemCompleterContext()) == getTypeParameterByVariable(typeParameter) && !it.isMarkedOption
            }
        }

        return isContainedInInvariantOrContravariantPositions(typeVariableConstructor, returnType)
                || dependingOnTypeParameter.any { isContainedInInvariantOrContravariantPositions(it, returnType) }
                || dependentTypeParameters.any { isContainedInInvariantOrContravariantPositions(it.first, returnType) }
                || (isContainedAnyDependentTypeInReturnType && isContainedInUpperBounds)
    }

    private fun TypeVariableFromCallableDescriptor.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter() {
        freshTypeConstructor.isContainedInInvariantOrContravariantPositions = true
    }

    override fun ResolutionCandidate.process(workIndex: Int) {
        for (variable in resolvedCall.freshVariablesSubstitutor.freshVariables) {
            val candidateDescriptor = resolvedCall.candidateDescriptor
            if (candidateDescriptor is ClassConstructorDescriptor) {
                val typeParameters = candidateDescriptor.containingDeclaration.declaredTypeParameters

                if (isContainedInInvariantOrContravariantPositionsAmongTypeParameters(variable, typeParameters)) {
                    variable.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter()
                }
            } else if (getSystem().isContainedInInvariantOrContravariantPositionsWithDependencies(
                    variable,
                    this.candidateDescriptor
                )
            ) {
                variable.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter()
            }
        }
    }
}

internal object CreateFreshVariablesSubstitutor : ResolutionPart() {
    fun TypeParameterDescriptor.shouldBeFlexible(flexibleCheck: (CangJieType) -> Boolean = { it.isFlexible() }): Boolean {
        return upperBounds.any {
            flexibleCheck(it) || ((it.constructor.declarationDescriptor as? TypeParameterDescriptor)?.run { shouldBeFlexible() }
                ?: false)
        }
    }

    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        candidateDescriptor: CallableDescriptor,
        cangjieCall: CangJieCall,
        csBuilder: ConstraintSystemOperation,
        typeParameters: List<TypeParameterDescriptor> = candidateDescriptor.typeParameters
    ): FreshVariableNewTypeSubstitutor {

        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }

        val toFreshVariables = FreshVariableNewTypeSubstitutor(freshTypeVariables)

        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        fun TypeVariableFromCallableDescriptor.addSubtypeConstraint(
            upperBound: CangJieType,
            position: DeclaredUpperBoundConstraintPositionImpl
        ) {
            csBuilder.addSubtypeConstraint(
                defaultType,
                toFreshVariables.safeSubstitute(upperBound.unwrap()),
                position
            )
        }

        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]
            val position = DeclaredUpperBoundConstraintPositionImpl(typeParameter, cangjieCall)

            for (upperBound in typeParameter.upperBounds) {
                freshVariable.addSubtypeConstraint(upperBound, position)
            }
        }

        if (candidateDescriptor is TypeAliasConstructorDescriptor) {
            val typeAliasDescriptor = candidateDescriptor.typeAliasDescriptor
            val originalTypes = typeAliasDescriptor.underlyingType.arguments.map { it.type }
            val originalTypeParameters = candidateDescriptor.underlyingConstructorDescriptor.typeParameters
            for (index in typeParameters.indices) {
                val typeParameter = typeParameters[index]
                val freshVariable = freshTypeVariables[index]
                val typeMapping = originalTypes.mapIndexedNotNull { i: Int, cangjieType: CangJieType ->
                    if (cangjieType == typeParameter.defaultType) i else null
                }
                for (originalIndex in typeMapping) {
                    // there can be null in case we already captured type parameter in outer class (in case of inner classes)
                    // see test innerClassTypeAliasConstructor.cj
                    val originalTypeParameter = originalTypeParameters.getOrNull(originalIndex) ?: continue
                    val position = DeclaredUpperBoundConstraintPositionImpl(originalTypeParameter, cangjieCall)
                    for (upperBound in originalTypeParameter.upperBounds) {
                        freshVariable.addSubtypeConstraint(upperBound, position)
                    }
                }
            }
        }
        return toFreshVariables
    }

    private fun getTypePreservingFlexibilityWrtTypeVariable(
        type: CangJieType,
        typeVariable: TypeVariableFromCallableDescriptor
    ): CangJieType {
        fun createFlexibleType() =
            CangJieTypeFactory.flexibleType(
                type.makeNotNullable().lowerIfFlexible(),
                type.makeOptional().upperIfFlexible()
            )

        return when {
            typeVariable.originalTypeParameter.shouldBeFlexible { it is FlexibleTypeWithEnhancement } ->
                createFlexibleType().wrapEnhancement(type)

            typeVariable.originalTypeParameter.shouldBeFlexible() -> createFlexibleType()
            else -> type
        }
    }

    private fun createKnownParametersFromFreshVariablesSubstitutor(
        freshVariableSubstitutor: FreshVariableNewTypeSubstitutor,
        knownTypeParametersSubstitutor: TypeSubstitutor,
    ): NewTypeSubstitutor {
        if (knownTypeParametersSubstitutor.isEmpty)
            return EmptySubstitutor

        val knownTypeParameterByTypeVariable = mutableMapOf<TypeConstructor, UnwrappedType>().let { map ->
            for (typeVariable in freshVariableSubstitutor.freshVariables) {
                val typeParameterType = typeVariable.originalTypeParameter.defaultType
                val substitutedKnownTypeParameter = knownTypeParametersSubstitutor.substitute(typeParameterType)

                if (substitutedKnownTypeParameter !== typeParameterType)
                    map[typeVariable.defaultType.constructor] = substitutedKnownTypeParameter
            }
            map
        }

        return knownTypeParametersSubstitutor.composeWith(
            NewTypeSubstitutorByConstructorMap(
                knownTypeParameterByTypeVariable
            )
        )
    }

    /**
     *
     */
    fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {

//        来自扩展
        val receiverTypeParameters = if (candidateDescriptor.isExtension) {
            if (candidateDescriptor is CallableDescriptorForExtend)
                (candidateDescriptor as CallableDescriptorForExtend).typeParametersForExtend
            else emptyList()
        } else {
            emptyList()
        }

//        如果接收器是DISPATCH_RECEIVER ，并且它是一个静态调用，可能要分析上一层的类型参数
        if (resolvedCall.dispatchReceiverArgument != null && resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

            return (resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver).classQualifier.descriptor.declaredTypeParameters + candidateDescriptor.original.typeParameters + receiverTypeParameters
        }

        return candidateDescriptor.original.typeParameters + receiverTypeParameters

    }

    override fun ResolutionCandidate.process(workIndex: Int) {
        val csBuilder = getSystem().getBuilder()
//        val toFreshVariables =
//            if (candidateDescriptor.typeParameters.isEmpty())
//                FreshVariableNewTypeSubstitutor.Empty
//            else
//                createToFreshVariableSubstitutorAndAddInitialConstraints(
//                    candidateDescriptor,
//                    resolvedCall.atom,
//                    csBuilder
//                )

        val typeParameters = getTypeParameters()
        val toFreshVariables =
            if (typeParameters.isEmpty())
                FreshVariableNewTypeSubstitutor.Empty
            else
                createToFreshVariableSubstitutorAndAddInitialConstraints(
                    candidateDescriptor,
                    resolvedCall.atom,
                    csBuilder,
                    getTypeParameters()
                )

        val knownTypeParametersSubstitutor = knownTypeParametersResultingSubstitutor?.let {
            createKnownParametersFromFreshVariablesSubstitutor(toFreshVariables, it)
        } ?: EmptySubstitutor

        resolvedCall.freshVariablesSubstitutor = toFreshVariables
        resolvedCall.knownParametersSubstitutor = knownTypeParametersSubstitutor
//        if (candidateDescriptor.typeParameters.isEmpty()) {
//            return
//        }
        if (typeParameters.isEmpty()) {
            return
        }

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) return

        // optimization
        if (resolvedCall.typeArgumentMappingByOriginal == TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments && knownTypeParametersResultingSubstitutor == null) {
            return
        }

//        val typeParameters = candidateDescriptor.original.typeParameters
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
//            TODO 会不会出现通过索引获取错误的情况，有待验证
            val freshVariable = toFreshVariables.freshVariables[index]

            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)
            if (knownTypeArgument != null) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(knownTypeArgument.unwrap(), freshVariable),
                    KnownTypeParameterConstraintPositionImpl(knownTypeArgument)
                )
                continue
            }

            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)

            if (typeArgument is SimpleTypeArgument) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(typeArgument.type, freshVariable),
                    ExplicitTypeParameterConstraintPositionImpl(typeArgument)
                )
            } else {
                assert(typeArgument == TypeArgumentPlaceholder) {
                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
                }
            }
        }
    }


}

internal object NoArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(cangjieCall.argumentsInParenthesis.isEmpty()) {
            "Variable call cannot has arguments: ${cangjieCall.argumentsInParenthesis}. Call: $cangjieCall"
        }
        assert(cangjieCall.externalArgument == null) {
            "Variable call cannot has external argument: ${cangjieCall.externalArgument}. Call: $cangjieCall"
        }
        resolvedCall.argumentMappingByOriginal = emptyMap()
        resolvedCall.argumentToCandidateParameter = emptyMap()
    }
}


internal object MapArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val mapping = callComponents.argumentsToParametersMapper.mapArguments(cangjieCall, candidateDescriptor)
        mapping.diagnostics.forEach(this::addDiagnostic)

        resolvedCall.argumentMappingByOriginal = mapping.parameterToCallArgumentMap

////        TODO 当没有使用()调用时，它是一个函数类型，不检查参数
//        if (/*cangjieCall.psiCangJieCall.psiCall.callElement !is CjCallExpression
//            && cangjieCall.psiCangJieCall.psiCall.callElement !is CjBinaryExpression
//            && cangjieCall.psiCangJieCall.psiCall.callElement !is CjCollectionLiteralExpression*/
//            cangjieCall.psiCangJieCall.psiCall.callElement is CjNameReferenceExpression
//            && !DescriptorUtils.isEnumEntry(this.candidateDescriptor)
//        ) {
//            resolvedCall.argumentMappingByOriginal = emptyMap()
//            return
//        }
//        val mapping = callComponents.argumentsToParametersMapper.mapArguments(cangjieCall, candidateDescriptor)
//        mapping.diagnostics.forEach(this::addDiagnostic)
//
//        resolvedCall.argumentMappingByOriginal = mapping.parameterToCallArgumentMap
    }
}

class ReceiverInfo(
    val isReceiver: Boolean,
    val shouldReportUnsafeCall: Boolean, // 如果已报告不安全的隐式调用，则不应报告
    val reportUnsafeCallAsUnsafeImplicitInvoke: Boolean,
    val selectorCall: CangJieCall? = null,
) {
    init {
        assert(!reportUnsafeCallAsUnsafeImplicitInvoke || shouldReportUnsafeCall) { "Inconsistent receiver info" }
    }

    companion object {
        val notReceiver = ReceiverInfo(
            isReceiver = false,
            shouldReportUnsafeCall = true,
            reportUnsafeCallAsUnsafeImplicitInvoke = false
        )
    }
}

object CaseEnumArgument : CangJieCallArgument {
    override val isSpread: Boolean = false
    override val argumentName: Name? = null

}

//当具有期望类型时
internal object CheckDesiredEnumType : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {

        if (scopeTower is PSICallResolver.ASTScopeTower) {
            val expectedType = (scopeTower as PSICallResolver.ASTScopeTower).context.expectedType

            val type = descriptor.returnType

            if (expectedType != NO_EXPECTED_TYPE && type?.let {
                    CangJieTypeChecker.DEFAULT.equalsIgnoringGenerics(
                        expectedType,
                        it
                    )
                } != true) {
                addDiagnostic(EmptyDiagnostic)
            }
        }


    }

}

internal object CheckCaseEnumArgumentSize : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {

        val dASize = candidateDescriptor.valueParameters.size
        val argSize = cangjieCall.argumentsInParenthesis.size
        if (argSize > dASize) {
//          参数过多

            addDiagnostic(TooManyArguments(CaseEnumArgument, candidateDescriptor))
        } else if (argSize < dASize) {
//          确实参数
//         未传递参数
            val args = candidateDescriptor.valueParameters.dropLast(dASize - argSize)
            args.forEach {
                addDiagnostic(NoValueForParameter(it, candidateDescriptor))

            }


        }


    }

    override fun ResolutionCandidate.workCount() = cangjieCall.argumentsInParenthesis.size
}

internal object CheckArgumentsInParenthesis : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val argument = cangjieCall.argumentsInParenthesis[workIndex]
        resolveCangJieArgument(
            argument,
            resolvedCall.argumentToCandidateParameter[argument],
            ReceiverInfo.notReceiver
        )
    }

    override fun ResolutionCandidate.workCount() = cangjieCall.argumentsInParenthesis.size
}

private fun ResolutionCandidate.resolveCangJieArgument(
    argument: CangJieCallArgument,
    candidateParameter: ParameterDescriptor?,
    receiverInfo: ReceiverInfo
) {
    val csBuilder = getSystem().getBuilder()
    val candidateExpectedType =
        candidateParameter?.let { argument.getExpectedType(it, callComponents.languageVersionSettings) }

    val isReceiver = receiverInfo.isReceiver
    val conversionDataBeforeSubtyping =
        if (isReceiver || candidateParameter == null || candidateExpectedType == null) {
            null
        } else {
            TypeConversions.performCompositeConversionBeforeSubtyping(
                this, argument, candidateParameter, candidateExpectedType
            )
        }

    val convertedExpectedType = conversionDataBeforeSubtyping?.convertedType
    val unsubstitutedExpectedType = conversionDataBeforeSubtyping?.convertedType ?: candidateExpectedType
    val expectedType = unsubstitutedExpectedType?.let { prepareExpectedType(it) }

    val convertedArgument =
        if (expectedType != null && !isReceiver && shouldRunConversionForConstants(expectedType)) {
            val convertedConstant = resolutionCallbacks.convertSignedConstantToUnsigned(argument)
            if (convertedConstant != null) {
                resolvedCall.registerArgumentWithConstantConversion(argument, convertedConstant)
            }

            convertedConstant
        } else null


    val inferenceSession = resolutionCallbacks.inferenceSession
    if (candidateExpectedType == null || // Nothing to convert
        convertedExpectedType != null || // Type is already converted
        isReceiver || // Receivers don't participate in conversions
        conversionDataBeforeSubtyping?.wasConversion == true || // We tried to convert type but failed
        conversionDataBeforeSubtyping?.conversionDefinitelyNotNeeded == true ||
        csBuilder.hasContradiction
    ) {
        val resolvedAtom = resolveCjPrimitive(
            csBuilder,
            argument,
            expectedType,
            this,
            receiverInfo,
            convertedArgument?.unknownIntegerType?.unwrap(),
            inferenceSession,
            selectorCall = receiverInfo.selectorCall
        )

        addResolvedCjPrimitive(resolvedAtom)
    } else {
        var convertedTypeAfterSubtyping: UnwrappedType? = null
        csBuilder.runTransaction {
            val resolvedAtom = resolveCjPrimitive(
                csBuilder,
                argument,
                expectedType,
                this@resolveCangJieArgument,
                receiverInfo,
                convertedArgument?.unknownIntegerType?.unwrap(),
                inferenceSession
            )

            if (!hasContradiction) {
                addResolvedCjPrimitive(resolvedAtom)
                return@runTransaction true
            }

            convertedTypeAfterSubtyping =
                TypeConversions.performCompositeConversionAfterSubtyping(
                    this@resolveCangJieArgument,
                    argument,
                    candidateParameter,
                    candidateExpectedType
                )?.let { prepareExpectedType(it) }

            if (convertedTypeAfterSubtyping == null) {
                addResolvedCjPrimitive(resolvedAtom)
                return@runTransaction true
            }

            false
        }

        if (convertedTypeAfterSubtyping != null) {
            val resolvedAtom = resolveCjPrimitive(
                csBuilder,
                argument,
                convertedTypeAfterSubtyping,
                this@resolveCangJieArgument,
                receiverInfo,
                convertedArgument?.unknownIntegerType?.unwrap(),
                inferenceSession
            )
            addResolvedCjPrimitive(resolvedAtom)
        }

    }
}

private fun ResolutionCandidate.shouldRunConversionForConstants(expectedType: UnwrappedType): Boolean {
    if (UnsignedTypes.isUnsignedType(expectedType)) return true
    val csBuilder = getSystem().getBuilder()
    if (csBuilder.isTypeVariable(expectedType)) {
        val variableWithConstraints =
            csBuilder.currentStorage().notFixedTypeVariables[expectedType.constructor] ?: return false
        return variableWithConstraints.constraints.any {
            it.kind == ConstraintKind.EQUALITY &&
                    it.position.from is ExplicitTypeParameterConstraintPositionImpl &&
                    UnsignedTypes.isUnsignedType(it.type as UnwrappedType)

        }
    }

    return false
}

/**
 * 根据预期类型准备实际类型
 * 此函数通过应用当前解析上下文中的变量替换和参数替换，来调整预期类型
 *
 * @param expectedType 预期的类型，即函数调用或表达式期望返回的类型
 * @return 调整后的类型，即经过变量和参数替换后，预期类型在当前上下文中的实际表示
 */
private fun ResolutionCandidate.prepareExpectedType(expectedType: UnwrappedType): UnwrappedType {
    // 使用当前解析调用的变量替换器，安全地替换预期类型中的泛型变量
    val resultType = resolvedCall.freshVariablesSubstitutor.safeSubstitute(expectedType)
    // 使用当前解析调用的已知参数替换器，安全地替换resultType中的参数类型
    return resolvedCall.knownParametersSubstitutor.safeSubstitute(resultType)
}


internal object ErrorDescriptorResolutionPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(ErrorUtils.isError(candidateDescriptor)) {
            "Should be error descriptor: $candidateDescriptor"
        }
        resolvedCall.typeArgumentMappingByOriginal =
            TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
        resolvedCall.argumentMappingByOriginal = emptyMap()
        resolvedCall.freshVariablesSubstitutor = FreshVariableNewTypeSubstitutor.Empty
        resolvedCall.knownParametersSubstitutor = EmptySubstitutor
        resolvedCall.argumentToCandidateParameter = emptyMap()

//        (cangjieCall.explicitReceiver as? SimpleCangJieCallArgument)?.let {
//            resolveCangJieArgument(it, null, ReceiverInfo.notReceiver)
//        }
//        for (argument in cangjieCall.argumentsInParenthesis) {
//            resolveCangJieArgument(argument, null, ReceiverInfo.notReceiver)
//        }
//
//        cangjieCall.externalArgument?.let {
//            resolveCangJieArgument(it, null, ReceiverInfo.notReceiver)
//        }
    }
}

internal object ArgumentsToCandidateParameterDescriptor : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val map = hashMapOf<CangJieCallArgument, ValueParameterDescriptor>()
        for ((originalValueParameter, resolvedCallArgument) in resolvedCall.argumentMappingByOriginal) {
            val valueParameter =
                candidateDescriptor.valueParameters.getOrNull(originalValueParameter.index) ?: continue
            for (argument in resolvedCallArgument.arguments) {
                map[argument] = valueParameter
            }
        }
        resolvedCall.argumentToCandidateParameter = map.compactIfPossible()
    }
}

internal object CheckExternalArgument : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val argument = cangjieCall.externalArgument ?: return

        resolveCangJieArgument(
            argument,
            resolvedCall.argumentToCandidateParameter[argument],
            ReceiverInfo.notReceiver
        )
    }
}

enum class DescriptorKind(val kind: String) {
    PROPERTY("property"),
    VARIABLE("variable"),
    FUNCTION("method"),
    ENUM("enum"),
    ENUM_ENTRY("enum entry"),
    UNKNOWN("unknown")
}

// 提取的方法
fun DeclarationDescriptor.getDescriptorKind(): DescriptorKind {
    return when (this) {
        is PropertyDescriptor -> DescriptorKind.PROPERTY
        is VariableDescriptor -> DescriptorKind.VARIABLE
        is FunctionDescriptor -> DescriptorKind.FUNCTION
        is EnumEntryDescriptor -> DescriptorKind.ENUM_ENTRY
        is LazyEnumDescriptor -> DescriptorKind.ENUM
        is EnumClassCallableDescriptor -> this.type.getDescriptorKind()
        else -> DescriptorKind.UNKNOWN
    }
}

internal object CheckIncompatibleTypeVariableUpperBounds : ResolutionPart() {
    /*
     * Check if the candidate was already discriminated by `CompatibilityOfTypeVariableAsIntersectionTypePart` resolution part
     * If it's true we shouldn't mark the candidate with warning, but should mark with error, to repeat the existing proper behaviour
     */
    private fun ResolutionCandidate.wasPreviouslyDiscriminated(upperTypes: List<CangJieTypeMarker>): Boolean {
        @Suppress("UNCHECKED_CAST")
        return callComponents.statelessCallbacks.isOldIntersectionIsEmpty(upperTypes as List<CangJieType>)
    }

    override fun ResolutionCandidate.process(workIndex: Int) =
        with(getSystem().asConstraintSystemCompleterContext()) {
            val constraintSystem = getSystem()
            for (variableWithConstraints in constraintSystem.getBuilder()
                .currentStorage().notFixedTypeVariables.values) {
                val upperTypes = variableWithConstraints.constraints.extractUpperTypesToCheckIntersectionEmptiness()

                when {
                    // TODO: consider reporting errors on bounded type variables by incompatible types but with other lower constraints
                    upperTypes.size <= 1 || variableWithConstraints.constraints.any { it.kind.isLower() } ->
                        continue

                    wasPreviouslyDiscriminated(upperTypes) -> {
                        markCandidateForCompatibilityResolve(needToReportWarning = false)
                        continue
                    }

                    (variableWithConstraints.typeVariable as? TypeVariableFromCallableDescriptor)?.originalTypeParameter?.let { parameter ->
                        resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(parameter)
                    } is SimpleTypeArgument -> continue

                    else -> {
                        val emptyIntersectionTypeInfo =
                            constraintSystem.getEmptyIntersectionTypeKind(upperTypes) ?: continue
//                    val isInferredEmptyIntersectionForbidden = callComponents.languageVersionSettings.supportsFeature(
//                        LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection
//                    )
                        val errorFactory = ::InferredEmptyIntersectionError
//                        if (isInferredEmptyIntersectionForbidden) ::InferredEmptyIntersectionError else ::InferredEmptyIntersectionWarning

                        addError(
                            errorFactory(
                                upperTypes,
                                emptyIntersectionTypeInfo.casingTypes.toList(),
                                variableWithConstraints.typeVariable,
                                emptyIntersectionTypeInfo.kind
                            )
                        )
                    }
                }
            }
        }
}

internal object CheckCallableReference : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        if (this !is CallableReferenceResolutionCandidate) {
            error("`CheckCallableReferences` resolution part is applicable only to callable reference calls")
        }

        val constraintSystem = getSystem().takeIf { !it.hasContradiction } ?: return

        addConstraints(constraintSystem.getBuilder(), resolvedCall.freshVariablesSubstitutor, cangjieCall)
    }
}
