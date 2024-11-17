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

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.scopes.ResolutionScope
import com.linqingying.cangjie.resolve.scopes.receivers.QualifierReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

fun <C : Candidate> C.forceResolution(): C {
    resultingApplicability
    return this
}

private val INAPPLICABLE_STATUSES = setOf(
    CandidateApplicability.INAPPLICABLE,
    CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,
    CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
)

val CandidateApplicability.isInapplicable: Boolean
    get() = this in INAPPLICABLE_STATUSES

val CallableDescriptor.isSynthesized: Boolean
    get() = (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED)

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

//对枚举进行展开
//internal class EnumEntryScopeTowerLevel(scopeTower: ImplicitScopeTower):AbstractScopeTowerLevel(scopeTower) {
//    override fun getVariables(
//        name: Name,
//        extensionReceiver: ReceiverValueWithSmartCastInfo?
//    ): Collection<CandidateWithBoundDispatchReceiver> {
//        return emptyList()
//    }
//
//    override fun getObjects(
//        name: Name,
//        extensionReceiver: ReceiverValueWithSmartCastInfo?
//    ): Collection<CandidateWithBoundDispatchReceiver> {
//        return emptyList()
//
//    }
//
//    override fun getFunctions(
//        name: Name,
//        extensionReceiver: ReceiverValueWithSmartCastInfo?
//    ): Collection<CandidateWithBoundDispatchReceiver> {
//        return emptyList()
//
//    }
//
//    override fun recordLookup(name: Name) {
//
//
//    }
//}

internal class QualifierScopeTowerLevel(scopeTower: ImplicitScopeTower, val qualifier: QualifierReceiver) :
    AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> = qualifier.staticScope
        .getContributedVariablesAndIntercept(
            name,
            location,
            qualifier.classValueReceiverWithSmartCastInfo,
            extensionReceiver,
            scopeTower
        ).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }

    override fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>  = qualifier.staticScope
        .getContributedFunctionsAndConstructors(
            name,
            location,
            qualifier.classValueReceiverWithSmartCastInfo,
            extensionReceiver,
            scopeTower
        ).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
    override fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?) = qualifier.staticScope
        .getContributedObjectVariables(name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }

    override fun getEnumTypeByKind(
        name: Name,
        kind: ClassKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return qualifier.staticScope
            .getContributedClassifiers(name, location).filter {
            (    it as? ClassDescriptor)?.kind == kind
            }.map {
                createCandidateDescriptor(

                    EnumClassCallableDescriptor(it)

                    , dispatchReceiver = null)
            }
    }

    override fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
return qualifier.staticScope
    .getContributedClassifiers(name, location).map {
        createCandidateDescriptor(  /*if(it is ClassDescriptor && it.kind == ClassKind.ENUM){
            it.unsubstitutedPrimaryConstructor!!

        }else{*/
            ClassCallableDescriptor(it)
//        }
        , dispatchReceiver = null)
    }

    }

    override fun recordLookup(name: Name) {

    }
}
private fun ResolutionScope.getContributedObjectVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
    val objectDescriptor = getFakeDescriptorForObject(getContributedClassifier(name, location))
    return listOfNotNull(objectDescriptor)
}
