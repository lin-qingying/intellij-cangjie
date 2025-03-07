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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor

enum class TypeUsage {
    SUPERTYPE, COMMON
}

open class ErasureTypeAttributes(
    // we use it to prevent happening a recursion while compute type parameter's upper bounds
    open val howThisTypeIsUsed: TypeUsage,
    open val visitedTypeParameters: Set<TypeParameterDescriptor>? = null,
    open val defaultType: SimpleType? = null
) {
    open fun withDefaultType(type: SimpleType?) = ErasureTypeAttributes(howThisTypeIsUsed, visitedTypeParameters, defaultType = type)

    open fun withNewVisitedTypeParameter(typeParameter: TypeParameterDescriptor) =
        ErasureTypeAttributes(
            howThisTypeIsUsed,
            visitedTypeParameters = visitedTypeParameters?.let { it + typeParameter } ?: setOf(typeParameter),
            defaultType
        )

    override fun equals(other: Any?): Boolean {
        if (other !is ErasureTypeAttributes) return false
        return other.defaultType == this.defaultType && other.howThisTypeIsUsed == this.howThisTypeIsUsed
    }

    override fun hashCode(): Int {
        var result = defaultType.hashCode()
        result += 31 * result + howThisTypeIsUsed.hashCode()
        return result
    }
}
