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

package org.cangnova.cangjie.types.expressions.match

import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.CangJieType

sealed class PatternKind {
    override fun toString(): String {


        return this::class.simpleName ?: ""
    }

    object Error : PatternKind()

    object Wild : PatternKind()

    /**
     * 绑定模式
     * let x:type
     */
    data class Binding(val type: CangJieType, val name: String) : PatternKind()
    data class Type(val type: CangJieType, val name: String) : PatternKind()

    /**
     * 常量模式
     */
    data class Const(val value: ConstantValue<*>) : PatternKind() {
        override fun showString(): String {
            return value.toString()
        }
    }

    /**
     * 元组模式
     */
    data class Tuple(val subPatterns: List<Pattern>) : PatternKind() {
        override fun showString(): String {
            val str = StringBuilder()

            str.append("(")
            str.append(
                subPatterns.joinToString(",") {
                    it.text(null)
                }
            )
            str.append(")")
            return str.toString()

        }
    }

    /**
     * 枚举模式
     */
    data class Enum(val enum: CjEnum, val entry: CjEnumConstructor, val subPatterns: List<Pattern>) : PatternKind() {
        override fun showString(): String {

            val str = StringBuilder()

            str.append(entry.name ?: "")
            if (subPatterns.isNotEmpty()) {
                str.append("(")
                str.append(
                    subPatterns.joinToString(",") {
                        it.text(null)
                    }
                )
                str.append(")")

            }
//            if (constructor.typeReferences.isNotEmpty()) {
//                str.append("(")
//                str.append(
//                    constructor.typeReferences.joinToString(",") {
//                        it.text
//                    }
//                )
//                str.append(")")
//
//            }

            return str.toString()


        }
    }


    open fun showString(): String {

        return ""
    }
}
