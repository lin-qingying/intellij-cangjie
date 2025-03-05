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

package cn.cangnova.cangjie.builtins

import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name

enum class PrimitiveType(typeName: String) {
    BOOL("Bool"),
    Rune("Rune"),

    INT64("Int64"),
    INT32("Int32"),
    INT16("Int16"),
    INT8("Int8"),
    INTNATIVE("IntNative"),
    UINT64("Int64"),
    UINT32("Int32"),
    UINT16("Int16"),
    UINT8("Int8"),
    UINTNATIVE("UIntNative"),

    FLOAT64("Float64"),
    FLOAT32("Float32"),
    FLOAT16("Float16"),

    ;

    val typeName: Name = Name.identifier(typeName)

    val arrayTypeName: Name = Name.identifier("${typeName}Array")

    val typeFqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        StandardNames.BUILT_INS_PACKAGE_FQ_NAME.child(
            this.typeName,
        )
    }

    val arrayTypeFqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        StandardNames.BUILT_INS_PACKAGE_FQ_NAME.child(
            arrayTypeName,
        )
    }

    companion object {
        @JvmField
        val NUMBER_TYPES = setOf(Rune, INT64, INT32, INT16, INT8, FLOAT64, FLOAT32, FLOAT16)

        @JvmStatic
        fun getByShortName(name: String): PrimitiveType? = when (name) {
            "Bool" -> BOOL
            "Rune" -> Rune

            "Int64" -> INT64

            "Int32" -> INT32

            "Int16" -> INT16

            "Int8" -> INT8
            "Float64" -> FLOAT64
            "Float32" -> FLOAT32
            "Float16" -> FLOAT16

            else -> null
        }

//        @JvmStatic
//        fun getByShortArrayName(name: String): PrimitiveType? = when (name) {
//            "BooleanArray" -> BOOLEAN
//            "CharArray" -> CHAR
//
//            "IntArray" -> INT64
//            "FloatArray" -> FLOAT
//
//            else -> null
//        }
    }
}
