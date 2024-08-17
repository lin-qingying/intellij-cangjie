package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.*
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.intellij.util.SmartList
import com.intellij.util.containers.addIfNotNull
internal class ImportingScopeBasedTowerLevel(
    scopeTower: ImplicitScopeTower,
    importingScope: ImportingScope
) : ScopeBasedTowerLevel(scopeTower, importingScope)

internal abstract class AbstractScopeTowerLevel(
    protected val scopeTower: ImplicitScopeTower
) : ScopeTowerLevel {
    protected val location: LookupLocation get() = scopeTower.location

    protected fun createCandidateDescriptor(
        descriptor: CallableDescriptor,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        specialError: ResolutionDiagnostic? = null,
        dispatchReceiverSmartCastType: CangJieType? = null
    ): CandidateWithBoundDispatchReceiver {
        val diagnostics = SmartList<ResolutionDiagnostic>()
        diagnostics.addIfNotNull(specialError)

//        if (ErrorUtils.isError(descriptor)) {
//            diagnostics.add(ErrorDescriptorDiagnostic)
//        } else {
//            if (descriptor.hasLowPriorityInOverloadResolution() || descriptor.isLowPriorityFromStdlibJre7Or8()) {
//                diagnostics.add(LowPriorityDescriptorDiagnostic)
//            }
//            if (dispatchReceiverSmartCastType != null) diagnostics.add(UsedSmartCastForDispatchReceiver(dispatchReceiverSmartCastType))
//
//            val shouldSkipVisibilityCheck = scopeTower.isNewInferenceEnabled
//            if (!shouldSkipVisibilityCheck) {
//                DescriptorVisibilityUtils.findInvisibleMember(
//                    getReceiverValueWithSmartCast(dispatchReceiver?.receiverValue, dispatchReceiverSmartCastType),
//                    descriptor,
//                    scopeTower.lexicalScope.ownerDescriptor,
//                    scopeTower.languageVersionSettings
//                )?.let { diagnostics.add(VisibilityError(it)) }
//            }
//        }
        return CandidateWithBoundDispatchReceiver(dispatchReceiver, descriptor, diagnostics)
    }

}
internal open class ScopeBasedTowerLevel protected constructor(
    scopeTower: ImplicitScopeTower,
    private val resolutionScope: ResolutionScope
) : AbstractScopeTowerLevel(scopeTower) {

    val deprecationDiagnosticOfThisScope: ResolutionDiagnostic? =
        if (resolutionScope is DeprecatedLexicalScope) ResolvedUsingDeprecatedVisibility(resolutionScope, location) else null

    internal constructor(scopeTower: ImplicitScopeTower, lexicalScope: LexicalScope) : this(scopeTower, lexicalScope as ResolutionScope)

    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
//        return emptyList()
       return resolutionScope.getContributedVariablesAndIntercept(
            name,
            location,
            null,
            extensionReceiver,
            scopeTower
        ).map {
            createCandidateDescriptor(
                it,
                dispatchReceiver = null,
                specialError = deprecationDiagnosticOfThisScope
            )
        }
    }

//    override fun getObjects(
//        name: Name,
//        extensionReceiver: ReceiverValueWithSmartCastInfo?
//    ): Collection<CandidateWithBoundDispatchReceiver> =
//        resolutionScope.getContributedObjectVariablesIncludeDeprecated(name, location).map { (classifier, isDeprecated) ->
//            createCandidateDescriptor(
//                classifier,
//                dispatchReceiver = null,
//                specialError = if (isDeprecated) ResolvedUsingDeprecatedVisibility(resolutionScope, location) else null
//            )
//        }

    override fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val result: ArrayList<CandidateWithBoundDispatchReceiver> = ArrayList()
//
        resolutionScope.getContributedFunctionsAndConstructors(name, location, null, extensionReceiver, scopeTower).mapTo(result) {
            createCandidateDescriptor(
                it,
                dispatchReceiver = null,
                specialError = deprecationDiagnosticOfThisScope
            )
        }

        // Add constructors of deprecated classifier with an additional diagnostic
        val descriptorWithDeprecation = resolutionScope.getContributedClassifierIncludeDeprecated(name, location)
        if (descriptorWithDeprecation != null && descriptorWithDeprecation.isDeprecated) {
            getConstructorsOfClassifier(descriptorWithDeprecation.descriptor).mapTo(result) {
                createCandidateDescriptor(
                    it,
                    dispatchReceiver = null,
                    specialError = ResolvedUsingDeprecatedVisibility(resolutionScope, location)
                )
            }
        }

        return result
    }

    override fun recordLookup(name: Name) {
        resolutionScope.recordLookup(name, location)
    }
}

private fun ResolutionScope.getContributedVariablesAndIntercept(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    extensionReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<VariableDescriptor> {
    val result = getContributedVariables(name, location)

    return scopeTower.interceptVariableCandidates(this, name, result, location, dispatchReceiver, extensionReceiver)
}
// todo KT-9538 Unresolved inner class via subclass reference
// todo add static methods & fields with error
//internal class MemberScopeTowerLevel(
//    scopeTower: ImplicitScopeTower,
//    val dispatchReceiver: ReceiverValueWithSmartCastInfo
//) : AbstractScopeTowerLevel(scopeTower) {
//
//    private val syntheticScopes = scopeTower.syntheticScopes
//    private val isNewInferenceEnabled = scopeTower.isNewInferenceEnabled
//    private val typeApproximator = scopeTower.typeApproximator
//
//    private fun collectMembers(
//        getMembers: ResolutionScope.(CangJieType?) -> Collection<CallableDescriptor>
//    ): Collection<CandidateWithBoundDispatchReceiver> {
//        val receiverValue = dispatchReceiver.receiverValue
//        val memberScope = receiverValue.type.memberScope
//
//        if (receiverValue.type is AbstractStubType && memberScope is ErrorScope && memberScope !is ThrowingScope) {
//            return arrayListOf()
//        }
//
//        val result = ArrayList<CandidateWithBoundDispatchReceiver>(0)
//
//        receiverValue.type.memberScope.getMembers(receiverValue.type).mapTo(result) {
//            createCandidateDescriptor(it, dispatchReceiver)
//        }
//
//        val unstableError = if (dispatchReceiver.isStable) null else UnstableSmartCastDiagnostic
//        val unstableCandidates = if (unstableError != null) ArrayList<CandidateWithBoundDispatchReceiver>(0) else null
//
//        for (possibleType in dispatchReceiver.typesFromSmartCasts) {
//            possibleType.memberScope.getMembers(possibleType).mapTo(unstableCandidates ?: result) {
//                createCandidateDescriptor(
//                    it,
//                    dispatchReceiver.smartCastReceiver(possibleType),
//                    unstableError, dispatchReceiverSmartCastType = possibleType
//                )
//            }
//        }
//
//        if (dispatchReceiver.hasTypesFromSmartCasts()) {
//            if (unstableCandidates == null) {
//                result.retainAll(result.selectMostSpecificInEachOverridableGroup { descriptor.approximateCapturedTypes(typeApproximator) })
//            } else {
//                result.addAll(
//                    unstableCandidates.selectMostSpecificInEachOverridableGroup { descriptor.approximateCapturedTypes(typeApproximator) }
//                )
//            }
//        }
//
//        if (receiverValue.type.isDynamic()) {
//            scopeTower.dynamicScope.getMembers(null).mapTo(result) {
//                createCandidateDescriptor(it, dispatchReceiver, DynamicDescriptorDiagnostic)
//            }
//        }
//
//        return result
//    }
//
//    /**
//     * this is bad hack for test like BlackBoxCodegenTestGenerated.Reflection.Properties#testGetPropertiesMutableVsReadonly (see last get call)
//     * Main reason for this hack: when we have List<*> we do capturing and transform receiver type to List<Capture(*)>.
//     * So method get has signature get(Int): Capture(*). If we also have smartcast to MutableList<String>, then there is also method get(Int): String.
//     * And we should chose get(Int): String.
//     */
//    private fun CallableDescriptor.approximateCapturedTypes(approximator: TypeApproximator): CallableDescriptor {
//        if (!isNewInferenceEnabled) return this
//
//        val wrappedSubstitution = object : TypeSubstitution() {
//            override fun get(key: CangJieType): TypeProjection? = null
//            override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) = when (position) {
//                Variance.INVARIANT -> null
//                Variance.OUT_VARIANCE -> approximator.approximateToSuperType(
//                    topLevelType.unwrap(),
//                    TypeApproximatorConfiguration.InternalTypesApproximation
//                )
//                Variance.IN_VARIANCE -> approximator.approximateToSubType(
//                    topLevelType.unwrap(),
//                    TypeApproximatorConfiguration.InternalTypesApproximation
//                )
//            } ?: topLevelType
//        }
//        return substitute(TypeSubstitutor.create(wrappedSubstitution))
//    }
//
//    private fun ReceiverValueWithSmartCastInfo.smartCastReceiver(targetType: CangJieType): ReceiverValueWithSmartCastInfo {
//        if (receiverValue !is ImplicitClassReceiver) return this
//
//        val newReceiverValue = CastImplicitClassReceiver(receiverValue.classDescriptor, targetType)
//        return ReceiverValueWithSmartCastInfo(newReceiverValue, typesFromSmartCasts, isStable)
//    }
//
//    override fun getVariables(
//        name: Name,
//        extensionReceiver: ReceiverValueWithSmartCastInfo?
//    ): Collection<CandidateWithBoundDispatchReceiver> {
//        return collectMembers { getContributedVariablesAndIntercept(name, location, dispatchReceiver, extensionReceiver, scopeTower) }
//    }
//
//    override fun getObjects(
//        name: Name,
//        extensionReceiver: ReceiverValueWithSmartCastInfo?
//    ): Collection<CandidateWithBoundDispatchReceiver> {
//        return emptyList()
//    }
//
//    override fun getFunctions(
//        name: Name,
//        extensionReceiver: ReceiverValueWithSmartCastInfo?
//    ): Collection<CandidateWithBoundDispatchReceiver> {
//        return collectMembers {
//            getContributedFunctionsAndIntercept(name, location, dispatchReceiver, extensionReceiver, scopeTower) + it.getInnerConstructors(
//                name,
//                location
//            ) + syntheticScopes.collectSyntheticMemberFunctions(listOfNotNull(it), name, location)
//        }
//    }
//
//    override fun recordLookup(name: Name) {
//        for (type in dispatchReceiver.allOriginalTypes) {
//            type.memberScope.recordLookup(name, location)
//        }
//    }
//}
private fun ResolutionScope.getContributedFunctionsAndConstructors(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    extensionReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<FunctionDescriptor> {
    val contributedFunctions = getContributedFunctions(name, location)

    val result = ArrayList<FunctionDescriptor>(contributedFunctions)

    getContributedClassifier(name, location)?.let {
        result.addAll(getConstructorsOfClassifier(it))
        result.addAll(scopeTower.syntheticScopes.collectSyntheticConstructors(it, location))
    }

    if (contributedFunctions.isNotEmpty()) {
        result.addAll(scopeTower.syntheticScopes.collectSyntheticStaticFunctions(contributedFunctions, location))
    }

    return scopeTower.interceptFunctionCandidates(this, name, result, location, dispatchReceiver, extensionReceiver)
}

private fun getConstructorsOfClassifier(classifier: ClassifierDescriptor?): List<ConstructorDescriptor> {
    val callableConstructors = when (classifier) {
        is TypeAliasDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        is ClassDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        else -> emptyList()
    }

    return callableConstructors.filter { it.dispatchReceiverParameter == null }
}
private val ClassDescriptor.canHaveCallableConstructors: Boolean
    get() = !ErrorUtils.isError(this) && !kind.isSingleton

private val TypeAliasDescriptor.canHaveCallableConstructors: Boolean
    get() = classDescriptor != null && !ErrorUtils.isError(classDescriptor) && classDescriptor!!.canHaveCallableConstructors
