package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.ResolutionScope
import com.huawei.cangjie.resolve.scopes.receivers.QualifierReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

fun <C : Candidate> C.forceResolution(): C {
    resultingApplicability
    return this
}
val CandidateWithBoundDispatchReceiver.requiresExtensionReceiver: Boolean
    get() = descriptor.extensionReceiverParameter != null
//private fun ResolutionScope.getContributedFunctionsAndConstructors(
//    name: Name,
//    location: LookupLocation,
//    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
//    extensionReceiver: ReceiverValueWithSmartCastInfo?,
//    scopeTower: ImplicitScopeTower
//): Collection<FunctionDescriptor> {
//    val contributedFunctions = getContributedFunctions(name, location)
//
//    val result = ArrayList<FunctionDescriptor>(contributedFunctions)
//
//    getContributedClassifier(name, location)?.let {
//        result.addAll(getConstructorsOfClassifier(it))
//        result.addAll(scopeTower.syntheticScopes.collectSyntheticConstructors(it, location))
//    }
//
//    if (contributedFunctions.isNotEmpty()) {
//        result.addAll(scopeTower.syntheticScopes.collectSyntheticStaticFunctions(contributedFunctions, location))
//    }
//
//    return scopeTower.interceptFunctionCandidates(this, name, result, location, dispatchReceiver, extensionReceiver)
//}
internal class QualifierScopeTowerLevel(scopeTower: ImplicitScopeTower, val qualifier: QualifierReceiver) :
    AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        TODO("Not yet implemented")
    }

    override fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        TODO("Not yet implemented")
    }


    //    override fun getFunctions(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?) = qualifier.staticScope
//        .getContributedFunctionsAndConstructors(
//            name,
//            location,
//            qualifier.classValueReceiverWithSmartCastInfo,
//            extensionReceiver,
//            scopeTower
//        ).map {
//            createCandidateDescriptor(it, dispatchReceiver = null)
//        }
    override fun recordLookup(name: Name) {
        TODO("Not yet implemented")
    }
}
