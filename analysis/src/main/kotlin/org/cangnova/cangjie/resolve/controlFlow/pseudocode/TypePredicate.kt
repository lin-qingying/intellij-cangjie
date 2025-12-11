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


package org.cangnova.cangjie.resolve.controlFlow.pseudocode

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import com.intellij.util.SmartFMap
import org.cangnova.cangjie.types.TypeUtils

interface TypePredicate : (CangJieType) -> Boolean {
    override fun invoke(typeToCheck: CangJieType): Boolean
}

data class SingleType(val targetType: CangJieType) : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean =
        CangJieTypeChecker.DEFAULT.equalTypes(typeToCheck, targetType)

    override fun toString(): String = targetType.render()
}

data class AllSubtypes(val upperBound: CangJieType) : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean =
        CangJieTypeChecker.DEFAULT.isSubtypeOf(typeToCheck, upperBound)

    override fun toString(): String = "{<: ${upperBound.render()}}"
}

data class ForAllTypes(val typeSets: List<TypePredicate>) : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean = typeSets.all { it(typeToCheck) }

    override fun toString(): String = "AND{${typeSets.joinToString(", ")}}"
}

data class ForSomeType(val typeSets: List<TypePredicate>) : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean = typeSets.any { it(typeToCheck) }

    override fun toString(): String = "OR{${typeSets.joinToString(", ")}}"
}

object AllTypes : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean = true

    override fun toString(): String = "*"
}

// todo: simplify computed type predicate when possible
fun and(predicates: Collection<TypePredicate>): TypePredicate =
    when (predicates.size) {
        0 -> AllTypes
        1 -> predicates.first()
        else -> ForAllTypes(predicates.toList())
    }

fun or(predicates: Collection<TypePredicate>): TypePredicate? =
    when (predicates.size) {
        0 -> null
        1 -> predicates.first()
        else -> ForSomeType(predicates.toList())
    }

fun CangJieType.getSubtypesPredicate(): TypePredicate = when {
    CangJieBuiltIns.isAny(this) && isOption -> AllTypes
    TypeUtils.canHaveSubtypes(CangJieTypeChecker.DEFAULT, this) -> AllSubtypes(this)
    else -> SingleType(this)
}


private fun CangJieType.render(): String = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(this)

fun <T : Any> TypePredicate.expectedTypeFor(keys: Iterable<T>): Map<T, TypePredicate> =
    keys.fold(SmartFMap.emptyMap<T, TypePredicate>()) { map, key -> map.plus(key, this) }
