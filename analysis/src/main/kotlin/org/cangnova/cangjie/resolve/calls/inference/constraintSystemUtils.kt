/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.cangnova.cangjie.resolve.calls.inference.constraintPosition.derivedFrom
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariable
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeArgument
import org.cangnova.cangjie.types.TypeArgumentImpl
import java.util.*

fun ConstraintSystem.getNestedTypeVariables(type: CangJieType): List<TypeVariable> {
    val nestedTypeParameters = type.getNestedTypeParameters().toSet()
    return typeVariables.filter { it.originalTypeParameter in nestedTypeParameters }
}

fun ConstraintSystem.filterConstraintsOut(excludePositionKind: ConstraintPositionKind): ConstraintSystem {
    return toBuilder { !it.derivedFrom(excludePositionKind) }.build()
}

internal fun CangJieType.getNestedTypeParameters(): List<TypeParameterDescriptor> {
    return getNestedArguments().mapNotNull { typeArgument ->
        typeArgument.type.constructor.declarationDescriptor as? TypeParameterDescriptor
    }
}

internal fun CangJieType.getNestedArguments(): List<TypeArgument> {
    val result = ArrayList<TypeArgument>()

    val stack = ArrayDeque<TypeArgument>()
    stack.push(TypeArgumentImpl(this))

    while (!stack.isEmpty()) {
        val typeArgument = stack.pop()


        result.add(typeArgument)

        typeArgument.type.arguments.forEach { stack.add(it) }
    }
    return result
}
