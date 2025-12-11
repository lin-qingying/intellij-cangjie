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

package org.cangnova.cangjie.resolve.scopes.receivers

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.types.checker.prepareArgumentTypeRegardingCaptureTypes
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.CangJieType

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


/**
 * 表示带有智能转换信息的接收者值。
 * 该类用于跟踪接收者值及其智能转换类型信息。
 *
 * @param receiverValue 接收者值。
 * @param typesFromSmartCasts 智能转换得到的类型集合，不包括 receiverValue.type，
 *                            仅用于特殊标记这些类型（例如，IDE绿色高亮显示），但不用于构造最终类型。
 * @param isStable 表示接收者值是否稳定。
 * @param originalBaseType 原始基础类型，默认为 receiverValue.type。
 */
class ReceiverValueWithSmartCastInfo(
    val receiverValue: ReceiverValue,
    val typesFromSmartCasts: Set<CangJieType>,
    val isStable: Boolean,
    originalBaseType: CangJieType = receiverValue.type
) : DetailedReceiver {
    // 用于构造最终类型
    val allOriginalTypes = typesFromSmartCasts + originalBaseType

    /**
     * 检查是否存在智能转换类型。
     *
     * @return 如果存在智能转换类型则返回 true，否则返回 false。
     */
    fun hasTypesFromSmartCasts() = typesFromSmartCasts.isNotEmpty()

    override fun toString() = receiverValue.toString()
}


fun ReceiverValueWithSmartCastInfo.prepareReceiverRegardingCaptureTypes(): ReceiverValueWithSmartCastInfo {
    val preparedBaseType = prepareArgumentTypeRegardingCaptureTypes(receiverValue.type.unwrap()) ?: return this

    return ReceiverValueWithSmartCastInfo(
        receiverValue.replaceType(preparedBaseType),
        typesFromSmartCasts,
        isStable,
        receiverValue.type
    )
}
