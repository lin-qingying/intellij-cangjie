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

package org.cangnova.cangjie.resolve.calls.util

import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.isBuiltinExtensionFunctionalType
import org.cangnova.cangjie.types.isBuiltinFunctionalType


fun createValueParametersForInvokeInFunctionType(
    functionDescriptor: FunctionDescriptor, parameterTypes: List<TypeProjection>
): List<ValueParameterDescriptor> {
    return parameterTypes.mapIndexed { i, typeProjection ->
        ValueParameterDescriptorImpl(
            functionDescriptor, null, i, Annotations.EMPTY,
            Name.identifier("p${i + 1}"), false, typeProjection.type,
            /* declaresDefaultValue = */ false,
            /* isCrossinline = */
//            false,
//            /* isNoinline = */ false,
//            null,
            SourceElement.NO_SOURCE
        )
    }
}

fun getValueParametersCountFromFunctionType(type: CangJieType): Int {
    assert(type.isBuiltinFunctionalType) { "Not a function type: $type" }
    // Function type arguments = receiver? + parameters + return-type
    return type.arguments.size - (if (type.isBuiltinExtensionFunctionalType) 1 else 0) - 1
}
