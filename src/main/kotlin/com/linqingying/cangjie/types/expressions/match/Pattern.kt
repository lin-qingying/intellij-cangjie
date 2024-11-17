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

package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils

private fun CjEnumEntry.initializer(subPatterns: List<Pattern>, ctx: CjElement?): String = when {

    typeEntry != null -> subPatterns.joinToString(",", "(", ")") { it.text(ctx) }
    else -> ""
}

data class Pattern(val type: CangJieType, val kind: PatternKind) {

    fun text(ctx: CjElement?): String =
        when (kind) {
            is PatternKind.Wild -> "_"

            is PatternKind.Binding -> kind.name

            is PatternKind.Enum -> {
                val entryName = kind.entry.name.orEmpty()

                val initializer = kind.entry.initializer(kind.subPatterns, ctx)
                "$entryName$initializer"
            }

            is PatternKind.Type -> {
                kind.name

            }

            is PatternKind.Const -> kind.value.toString()
            PatternKind.Error -> ""
            is PatternKind.Tuple -> kind.subPatterns.joinToString(",", "(", ")") { it.text(ctx) }
        }

    val constructors: List<Constructor>?
        get() = when (kind) {
            is PatternKind.Wild, is PatternKind.Binding -> null
            is PatternKind.Enum -> listOf(Constructor.Enum(kind.entry))

            is PatternKind.Const -> listOf(Constructor.ConstantValue(kind.value))
            is PatternKind.Tuple -> listOf(Constructor.Single)

            is PatternKind.Error -> null
            is PatternKind.Type -> listOf(Constructor.Type(kind.type))
        }

    /**
     * Returns the type of the pattern suitable for generating constructors
     *
     * @returns dereferenced [type] when [type] is a (multi)reference to enum
     * @returns [type] in other cases
     */
    val ergonomicType: CangJieType
        get() {

            return type
        }

    companion object {
        val Error = Pattern(ErrorUtils.errorVariableType, PatternKind.Error)

        fun wild(ty: CangJieType = ErrorUtils.errorVariableType): Pattern = Pattern(ty, PatternKind.Wild)

    }
}
