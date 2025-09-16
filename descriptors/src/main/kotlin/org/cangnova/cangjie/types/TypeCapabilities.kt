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

package org.cangnova.cangjie.types


// To facilitate laziness, any CangJieType implementation may inherit from this trait,
// even if it turns out that the type an instance represents is not actually a type parameter
// (i.e. it is not derived from a type parameter), see isTypeParameter
interface CustomTypeParameter {
    val isTypeParameter: Boolean

    // Throws an exception when isTypeParameter == false
    fun substitutionResult(replacement: CangJieType): CangJieType
}
// That interface is needed to provide information about definitely not null

//   type parameters (e.g. from @NotNull annotation) to type system
interface NonOptionTypeParameter : CustomTypeParameter
fun CangJieType.getCustomTypeParameter(): CustomTypeParameter? =
    (unwrap() as? CustomTypeParameter)?.let {
        if (it.isTypeParameter) it else null
    }
fun sameTypeConstructors(first: CangJieType, second: CangJieType): Boolean {
    return (first.unwrap() as? SubtypingRepresentatives)?.sameTypeConstructor(second) ?: false
            || (second.unwrap() as? SubtypingRepresentatives)?.sameTypeConstructor(first) ?: false
}
fun CangJieType.getSubtypeRepresentative(): CangJieType =
    (unwrap() as? SubtypingRepresentatives)?.subTypeRepresentative ?: this
fun CangJieType.getSupertypeRepresentative(): CangJieType =
    (unwrap() as? SubtypingRepresentatives)?.superTypeRepresentative ?: this
fun CangJieType.isCustomTypeParameter(): Boolean = (unwrap() as? CustomTypeParameter)?.isTypeParameter ?: false
