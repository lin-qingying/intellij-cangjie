/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.ResolutionScope
import org.cangnova.cangjie.resolve.scopes.receivers.QualifierReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.utils.isExtension

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

/**
 * 判断候选函数是否需要扩展接收者
 *
 * 这个属性表示当前候选函数是否定义了扩展接收者参数
 * 扩展接收者参数用于指定一个类型，该类型的成员可以通过扩展函数或扩展属性直接调用
 *
 * @return Boolean 如果候选函数有扩展接收者参数，则返回true；否则返回false
 */
val CandidateWithBoundDispatchReceiver.requiresExtensionReceiver: Boolean
    get() = descriptor.isExtension


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
    ): Collection<CandidateWithBoundDispatchReceiver> = qualifier.staticScope
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
                (it as? ClassDescriptor)?.kind == kind
            }.map {
                createCandidateDescriptor(

                    EnumClassCallableDescriptor(it), dispatchReceiver = null
                )
            }
    }

    override fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return qualifier.staticScope
            .getContributedClassifiers(name, location).map {
                createCandidateDescriptor(
                    ClassCallableDescriptor(it), dispatchReceiver = null
                )
            }

    }

    override fun getEnumEntrys(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return qualifier.staticScope
            .getContributedEnumEntrys(name, location).map {
                createCandidateDescriptor(

                    EnumClassCallableDescriptor(it), dispatchReceiver = null
                )
            }
    }

    override fun recordLookup(name: Name) {

    }
}

private fun ResolutionScope.getContributedObjectVariables(
    name: Name,
    location: LookupLocation
): Collection<VariableDescriptor> {
    val objectDescriptor = getFakeDescriptorForObject(getContributedClassifier(name, location))
    return listOfNotNull(objectDescriptor)
}
