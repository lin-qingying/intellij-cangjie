package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.ResolutionScope
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.utils.SmartList
import com.intellij.util.containers.addIfNotNull

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

//    val deprecationDiagnosticOfThisScope: ResolutionDiagnostic? =
//        if (resolutionScope is DeprecatedLexicalScope) ResolvedUsingDeprecatedVisibility(resolutionScope, location) else null

    internal constructor(scopeTower: ImplicitScopeTower, lexicalScope: LexicalScope) : this(scopeTower, lexicalScope as ResolutionScope)

    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return emptyList()
//       return resolutionScope.getContributedVariablesAndIntercept(
//            name,
//            location,
//            null,
//            extensionReceiver,
//            scopeTower
//        ).map {
//            createCandidateDescriptor(
//                it,
//                dispatchReceiver = null,
//                specialError = deprecationDiagnosticOfThisScope
//            )
//        }
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
//        resolutionScope.getContributedFunctionsAndConstructors(name, location, null, extensionReceiver, scopeTower).mapTo(result) {
//            createCandidateDescriptor(
//                it,
//                dispatchReceiver = null,
//                specialError = deprecationDiagnosticOfThisScope
//            )
//        }
//
//        // Add constructors of deprecated classifier with an additional diagnostic
//        val descriptorWithDeprecation = resolutionScope.getContributedClassifierIncludeDeprecated(name, location)
//        if (descriptorWithDeprecation != null && descriptorWithDeprecation.isDeprecated) {
//            getConstructorsOfClassifier(descriptorWithDeprecation.descriptor).mapTo(result) {
//                createCandidateDescriptor(
//                    it,
//                    dispatchReceiver = null,
//                    specialError = ResolvedUsingDeprecatedVisibility(resolutionScope, location)
//                )
//            }
//        }

        return result
    }

    override fun recordLookup(name: Name) {
        resolutionScope.recordLookup(name, location)
    }
}
