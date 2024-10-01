package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemOperation
import com.huawei.cangjie.resolve.calls.inference.components.*
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintKind
import com.huawei.cangjie.resolve.calls.inference.model.DeclaredUpperBoundConstraintPositionImpl
import com.huawei.cangjie.resolve.calls.inference.model.ExplicitTypeParameterConstraintPositionImpl
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import com.huawei.cangjie.resolve.calls.inference.runTransaction
import com.huawei.cangjie.resolve.calls.inference.substitute
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.VisibilityError
import com.huawei.cangjie.resolve.calls.tower.psiCangJieCall
import com.huawei.cangjie.resolve.calls.util.getReceiverValueWithSmartCast
import com.huawei.cangjie.resolve.isInsideInterface
import com.huawei.cangjie.resolve.isStatic
import com.huawei.cangjie.resolve.scopes.receivers.ClassQualifier
import com.huawei.cangjie.types.*
import com.huawei.cangjie.utils.compactIfPossible


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


internal object CheckStaticCall : ResolutionPart() {

    fun ResolvedCallAtom.isStaticContext(): Boolean {
        atom.explicitReceiver ?: return false
        return when (val value = atom.explicitReceiver!!.receiver) {

            is ClassQualifier -> {
                !(value.descriptor.kind == ClassKind.ENUM || value.descriptor.kind == ClassKind.ENUM_ENTRY)

            }

            else -> false
        }


    }

    override fun ResolutionCandidate.process(workIndex: Int) {
        val descriptor = this.descriptor


//        是否为static上下文
        val isStaticContext = this.resolvedCall.isStaticContext()

        val kind = when (descriptor) {
            is PropertyDescriptor -> "property"
            is VariableDescriptor -> "variable"
            is FunctionDescriptor -> "method"
            else -> "unknown"
        }
        val memberStatic = descriptor.isStatic()
//        非静态上下文访问静态成员
        if (memberStatic && !isStaticContext) {
            addDiagnostic(NonStaticContextAccessStaticMemberDiagnostic(kind, descriptor))
        }
//静态上下文访问非静成员
        if (!memberStatic && isStaticContext) {
            addDiagnostic(StaticContextAccessNonStaticMemberDiagnostic(kind, descriptor))
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
        csBuilder: ConstraintSystemOperation
    ): FreshVariableNewTypeSubstitutor {
        val typeParameters = candidateDescriptor.typeParameters

        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }

        val toFreshVariables = FreshVariableNewTypeSubstitutor(freshTypeVariables)

        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        fun TypeVariableFromCallableDescriptor.addSubtypeConstraint(
            upperBound: CangJieType,
            position: DeclaredUpperBoundConstraintPositionImpl
        ) {
            csBuilder.addSubtypeConstraint(defaultType, toFreshVariables.safeSubstitute(upperBound.unwrap()), position)
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

    override fun ResolutionCandidate.process(workIndex: Int) {
        val csBuilder = getSystem().getBuilder()
        val toFreshVariables =
            if (candidateDescriptor.typeParameters.isEmpty())
                FreshVariableNewTypeSubstitutor.Empty
            else
                createToFreshVariableSubstitutorAndAddInitialConstraints(
                    candidateDescriptor,
                    resolvedCall.atom,
                    csBuilder
                )

        val knownTypeParametersSubstitutor = knownTypeParametersResultingSubstitutor?.let {
            createKnownParametersFromFreshVariablesSubstitutor(toFreshVariables, it)
        } ?: EmptySubstitutor

        resolvedCall.freshVariablesSubstitutor = toFreshVariables
        resolvedCall.knownParametersSubstitutor = knownTypeParametersSubstitutor

        if (candidateDescriptor.typeParameters.isEmpty()) {
            return
        }

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) return

        // optimization
//        if (resolvedCall.typeArgumentMappingByOriginal == NoExplicitArguments && knownTypeParametersResultingSubstitutor == null) {
//            return
//        }

//        val typeParameters = candidateDescriptor.original.typeParameters
//        for (index in typeParameters.indices) {
//            val typeParameter = typeParameters[index]
//            val freshVariable = toFreshVariables.freshVariables[index]
//
//            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)
//            if (knownTypeArgument != null) {
//                csBuilder.addEqualityConstraint(
//                    freshVariable.defaultType,
//                    getTypePreservingFlexibilityWrtTypeVariable(knownTypeArgument.unwrap(), freshVariable),
//                    KnownTypeParameterConstraintPositionImpl(knownTypeArgument)
//                )
//                continue
//            }
//
//            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)
//
//            if (typeArgument is SimpleTypeArgument) {
//                csBuilder.addEqualityConstraint(
//                    freshVariable.defaultType,
//                    getTypePreservingFlexibilityWrtTypeVariable(typeArgument.type, freshVariable),
//                    ExplicitTypeParameterConstraintPositionImpl(typeArgument)
//                )
//            } else {
//                assert(typeArgument == TypeArgumentPlaceholder) {
//                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
//                }
//            }
//        }
    }

//    fun TypeParameterDescriptor.shouldBeFlexible(flexibleCheck: (CangJieType) -> Boolean = { it.isFlexible() }): Boolean {
//        return upperBounds.any {
//            flexibleCheck(it) || ((it.constructor.declarationDescriptor as? TypeParameterDescriptor)?.run { shouldBeFlexible() } ?: false)
//        }
//    }
//
//    private fun getTypePreservingFlexibilityWrtTypeVariable(
//        type: CangJieType,
//        typeVariable: TypeVariableFromCallableDescriptor
//    ): CangJieType {
//        fun createFlexibleType() =
//            CangJieTypeFactory.flexibleType(type.makeNotNullable().lowerIfFlexible(), type.makeOptional().upperIfFlexible())
//
//        return when {
//            typeVariable.originalTypeParameter.shouldBeFlexible { it is FlexibleTypeWithEnhancement } ->
//                createFlexibleType().wrapEnhancement(type)
//            typeVariable.originalTypeParameter.shouldBeFlexible() -> createFlexibleType()
//            else -> type
//        }
//    }
//
//    private fun createKnownParametersFromFreshVariablesSubstitutor(
//        freshVariableSubstitutor: FreshVariableNewTypeSubstitutor,
//        knownTypeParametersSubstitutor: TypeSubstitutor,
//    ): NewTypeSubstitutor {
//        if (knownTypeParametersSubstitutor.isEmpty)
//            return EmptySubstitutor
//
//        val knownTypeParameterByTypeVariable = mutableMapOf<TypeConstructor, UnwrappedType>().let { map ->
//            for (typeVariable in freshVariableSubstitutor.freshVariables) {
//                val typeParameterType = typeVariable.originalTypeParameter.defaultType
//                val substitutedKnownTypeParameter = knownTypeParametersSubstitutor.substitute(typeParameterType)
//
//                if (substitutedKnownTypeParameter !== typeParameterType)
//                    map[typeVariable.defaultType.constructor] = substitutedKnownTypeParameter
//            }
//            map
//        }
//
//        return knownTypeParametersSubstitutor.composeWith(NewTypeSubstitutorByConstructorMap(knownTypeParameterByTypeVariable))
//    }
//
//    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
//        candidateDescriptor: CallableDescriptor,
//        cangjieCall: CangJieCall,
//        csBuilder: ConstraintSystemOperation
//    ): FreshVariableNewTypeSubstitutor {
//        val typeParameters = candidateDescriptor.typeParameters
//
//        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }
//
//        val toFreshVariables = FreshVariableNewTypeSubstitutor(freshTypeVariables)
//
//        for (freshVariable in freshTypeVariables) {
//            csBuilder.registerVariable(freshVariable)
//        }
//
//        fun TypeVariableFromCallableDescriptor.addSubtypeConstraint(
//            upperBound: CangJieType,
//            position: DeclaredUpperBoundConstraintPositionImpl
//        ) {
//            csBuilder.addSubtypeConstraint(defaultType, toFreshVariables.safeSubstitute(upperBound.unwrap()), position)
//        }
//
//        for (index in typeParameters.indices) {
//            val typeParameter = typeParameters[index]
//            val freshVariable = freshTypeVariables[index]
//            val position = DeclaredUpperBoundConstraintPositionImpl(typeParameter, cangjieCall)
//
//            for (upperBound in typeParameter.upperBounds) {
//                freshVariable.addSubtypeConstraint(upperBound, position)
//            }
//        }
//
//        if (candidateDescriptor is TypeAliasConstructorDescriptor) {
//            val typeAliasDescriptor = candidateDescriptor.typeAliasDescriptor
//            val originalTypes = typeAliasDescriptor.underlyingType.arguments.map { it.type }
//            val originalTypeParameters = candidateDescriptor.underlyingConstructorDescriptor.typeParameters
//            for (index in typeParameters.indices) {
//                val typeParameter = typeParameters[index]
//                val freshVariable = freshTypeVariables[index]
//                val typeMapping = originalTypes.mapIndexedNotNull { i: Int, cangjieType: CangJieType ->
//                    if (cangjieType == typeParameter.defaultType) i else null
//                }
//                for (originalIndex in typeMapping) {
//                    // there can be null in case we already captured type parameter in outer class (in case of inner classes)
//                    // see test innerClassTypeAliasConstructor.cj
//                    val originalTypeParameter = originalTypeParameters.getOrNull(originalIndex) ?: continue
//                    val position = DeclaredUpperBoundConstraintPositionImpl(originalTypeParameter, cangjieCall)
//                    for (upperBound in originalTypeParameter.upperBounds) {
//                        freshVariable.addSubtypeConstraint(upperBound, position)
//                    }
//                }
//            }
//        }
//        return toFreshVariables
//    }
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

//internal object PostponedVariablesInitializerResolutionPart : ResolutionPart() {
//    override fun ResolutionCandidate.process(workIndex: Int) {
//        val csBuilder = getSystem().getBuilder()
//        for ((argument, parameter) in resolvedCall.argumentToCandidateParameter) {
//            if (!callComponents.statelessCallbacks.isBuilderInferenceCall(argument, parameter)) continue
//            val receiverType = parameter.type.getReceiverTypeFromFunctionType() ?: continue
//            val dontUseBuilderInferenceIfPossible =
//                callComponents.languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceOnlyIfNeeded)
//
//            if (argument is LambdaCangJieCallArgument && !argument.hasBuilderInferenceAnnotation) {
//                argument.hasBuilderInferenceAnnotation = true
//            }
//
//            if (dontUseBuilderInferenceIfPossible) continue
//
//            for (freshVariable in resolvedCall.freshVariablesSubstitutor.freshVariables) {
//                if (resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(freshVariable.originalTypeParameter) is SimpleTypeArgument)
//                    continue
//
//                if (csBuilder.isPostponedTypeVariable(freshVariable)) continue
//                if (receiverType.contains { it.constructor == freshVariable.originalTypeParameter.typeConstructor }) {
//                    csBuilder.markPostponedVariable(freshVariable)
//                }
//            }
//        }
//    }
//}
internal object MapArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {


//        TODO 当没有使用()调用时，它是一个函数类型，不检查参数
        if (/*cangjieCall.psiCangJieCall.psiCall.callElement !is CjCallExpression
            && cangjieCall.psiCangJieCall.psiCall.callElement !is CjBinaryExpression
            && cangjieCall.psiCangJieCall.psiCall.callElement !is CjCollectionLiteralExpression*/
            cangjieCall.psiCangJieCall.psiCall.callElement is CjNameReferenceExpression
        ) {
            resolvedCall.argumentMappingByOriginal = emptyMap()
            return
        }
        val mapping = callComponents.argumentsToParametersMapper.mapArguments(cangjieCall, candidateDescriptor)
        mapping.diagnostics.forEach(this::addDiagnostic)

        resolvedCall.argumentMappingByOriginal = mapping.parameterToCallArgumentMap
    }
}

class ReceiverInfo(
    val isReceiver: Boolean,
    val shouldReportUnsafeCall: Boolean, // should not report if unsafe implicit invoke has been reported already
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

internal object CheckArgumentsInParenthesis : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val argument = cangjieCall.argumentsInParenthesis[workIndex]
        resolveCangJieArgument(argument, resolvedCall.argumentToCandidateParameter[argument], ReceiverInfo.notReceiver)
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

    val convertedArgument = if (expectedType != null && !isReceiver && shouldRunConversionForConstants(expectedType)) {
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

private fun ResolutionCandidate.prepareExpectedType(expectedType: UnwrappedType): UnwrappedType {
    val resultType = resolvedCall.freshVariablesSubstitutor.safeSubstitute(expectedType)
    return resolvedCall.knownParametersSubstitutor.safeSubstitute(resultType)
}

internal object ErrorDescriptorResolutionPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(ErrorUtils.isError(candidateDescriptor)) {
            "Should be error descriptor: $candidateDescriptor"
        }
//        resolvedCall.typeArgumentMappingByOriginal = TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
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
            val valueParameter = candidateDescriptor.valueParameters.getOrNull(originalValueParameter.index) ?: continue
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

        resolveCangJieArgument(argument, resolvedCall.argumentToCandidateParameter[argument], ReceiverInfo.notReceiver)
    }
}
