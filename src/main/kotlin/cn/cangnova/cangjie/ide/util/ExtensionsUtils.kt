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

package cn.cangnova.cangjie.ide.util

import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeSubstitutor
import cn.cangnova.cangjie.types.fuzzyExtensionReceiverType
import cn.cangnova.cangjie.types.nullability
import cn.cangnova.cangjie.types.util.TypeNullability
import cn.cangnova.cangjie.types.util.makeNotNullable
import cn.cangnova.cangjie.types.util.nullability
import cn.cangnova.cangjie.utils.CallType


fun <TCallable : CallableDescriptor> TCallable.substituteExtensionIfCallable(
    receiverTypes: Collection<CangJieType>,
    callType: CallType<*>,
    ignoreTypeParameters: Boolean = false,
): Collection<TCallable> {
    if (!callType.descriptorKindFilter.accepts(this)) return listOf()

    var types = receiverTypes.asSequence()
    if (callType == CallType.SAFE) {
        types = types.map { it.makeNotNullable() }
    }

    val extensionReceiverType = fuzzyExtensionReceiverType()!!
    val substitutors = types.mapNotNull {
        // NOTE: this creates a fuzzy type for `it` without type parameters
        var substitutor = extensionReceiverType.checkIsSuperTypeOf(it)

        // If enabled, we can ignore type parameters in the receiver type, and only check whether the constructors match
        if (ignoreTypeParameters && substitutor == null && it.constructor == extensionReceiverType.type.constructor) {
            substitutor = TypeSubstitutor.EMPTY
        }

        // check if we may fail due to receiver expression being nullable
        if (substitutor == null && it.nullability() == TypeNullability.NULLABLE && extensionReceiverType.nullability() == TypeNullability.NOT_NULL) {
            substitutor = extensionReceiverType.checkIsSuperTypeOf(it.makeNotNullable())
        }
        substitutor
    }

    return if (typeParameters.isEmpty()) { // optimization for non-generic callables
        if (substitutors.any()) listOf(this) else listOf()
    } else {
        substitutors
            .mapNotNull { @Suppress("UNCHECKED_CAST") (substitute(it) as TCallable?) }
            .toList()
    }
}
