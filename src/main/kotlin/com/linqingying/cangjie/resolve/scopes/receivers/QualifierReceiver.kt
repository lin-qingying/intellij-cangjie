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

package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.types.checker.prepareArgumentTypeRegardingCaptureTypes
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.CangJieType

// this receiver used only for resolution. see subtypes
interface DetailedReceiver
interface QualifierReceiver : Receiver, DetailedReceiver {
    val descriptor: DeclarationDescriptor

    val staticScope: MemberScope

    val classValueReceiver: ReceiverValue?

    // for qualifiers smart cast is impossible
    val classValueReceiverWithSmartCastInfo: ReceiverValueWithSmartCastInfo?
        get() = classValueReceiver?.let { ReceiverValueWithSmartCastInfo(it, emptySet(), true) }
}


class ReceiverValueWithSmartCastInfo(
    val receiverValue: ReceiverValue,
    /*
     * It doesn't include receiver.type and is used only to special marking such types (e.g. for IDE green highlighting)
     * but not to construct the resulting type
     */
    val typesFromSmartCasts: Set<CangJieType>,
    val isStable: Boolean,
    originalBaseType: CangJieType = receiverValue.type
) : DetailedReceiver {
    // It's used to construct the resulting type
    val allOriginalTypes = typesFromSmartCasts + originalBaseType

    fun hasTypesFromSmartCasts() = typesFromSmartCasts.isNotEmpty()

    override fun toString() = receiverValue.toString()
}

fun ReceiverValueWithSmartCastInfo.prepareReceiverRegardingCaptureTypes(): ReceiverValueWithSmartCastInfo {
    val preparedBaseType = prepareArgumentTypeRegardingCaptureTypes(receiverValue.type.unwrap()) ?: return this

    return ReceiverValueWithSmartCastInfo(receiverValue.replaceType(preparedBaseType), typesFromSmartCasts, isStable, receiverValue.type)
}
