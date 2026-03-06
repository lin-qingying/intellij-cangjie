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

package org.cangnova.cangjie.name

import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableSet
import com.intellij.psi.tree.IElementType
import kotlin.collections.get

object OperatorConventions {

//    val DOUBLE: Name = Name.identifier("toDouble")
//
//    val FLOAT: Name = Name.identifier("toFloat")

//    val LONG: Name = Name.identifier("toLong")

//    val INT: Name = Name.identifier("toInt")
//
//    val CHAR: Name = Name.identifier("toChar")
//
//    val SHORT: Name = Name.identifier("toShort")
//
//    val BYTE: Name = Name.identifier("toByte")

    fun getOperationSymbolForName(name: Name): CjToken? {
        if (!isConventionName(name)) return null
        var token =
            BINARY_OPERATION_NAMES.inverse().get(name)
        if (token != null) return token
        token = UNARY_OPERATION_NAMES.inverse().get(name)
        if (token != null) return token
        token = ASSIGNMENT_OPERATIONS.inverse().get(name)
        if (token != null) return token
        return null
    }

    val NUMBER_CONVERSIONS: ImmutableSet<Name> =
        ImmutableSet.of<Name>(
//           OperatorConventions.DOUBLE,
//           OperatorConventions.FLOAT,
//            LONG,
//           OperatorConventions.INT,
//           OperatorConventions.SHORT,
//           OperatorConventions.BYTE,
//           OperatorConventions.CHAR
        )

    fun isConventionName(name: Name): Boolean {
        return CONVENTION_NAMES.contains(name)
    }

    @JvmField
    val ASSIGNMENT_OPERATION_COUNTERPARTS: ImmutableBiMap<CjSingleValueToken, CjSingleValueToken> =
        ImmutableBiMap.builder<CjSingleValueToken, CjSingleValueToken>()
            .put(CjTokens.MULTEQ, CjTokens.MUL)
            .put(CjTokens.DIVEQ, CjTokens.DIV)
            .put(CjTokens.PERCEQ, CjTokens.PERC)
            .put(CjTokens.PLUSEQ, CjTokens.PLUS)
            .put(CjTokens.MINUSEQ, CjTokens.MINUS)
            .put(CjTokens.ANDEQ, CjTokens.AND)
            .put(CjTokens.XOREQ, CjTokens.XOR)
            .put(CjTokens.OREQ, CjTokens.OR)
            .put(CjTokens.LTLTEQ, CjTokens.LTLT)
            .put(CjTokens.GTGTEQ, CjTokens.GTGT)
            .put(CjTokens.MULMULEQ, CjTokens.MULMUL)
            .put(CjTokens.ANDANDEQ, CjTokens.ANDAND)
            .put(CjTokens.OROREQ, CjTokens.OROR)
            .build()

    @JvmStatic
    fun isConventionType(type: IElementType): Boolean {
        return COMPARISON_OPERATIONS_NAMES.containsKey(type) ||
            UNARY_OPERATION_NAMES.containsKey(type) ||
            BINARY_OPERATION_NAMES.containsKey(type) || FLOW_OPERATION_NAMES.containsKey(type) ||
            COMPARISON_OPERATIONS_NAMES.containsKey(
                type,
            )
    }

    @JvmStatic
    fun getNameForOperationSymbol(token: CjToken?): Name? {
        token ?: return null
        return getNameForOperationSymbol(token, true, true)
    }

    @JvmField
    val COMPARISON_OPERATIONS_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.GTEQ, OperatorNameConventions.COMPARE_GTEQ)
            .put(CjTokens.LTEQ, OperatorNameConventions.COMPARE_LTEQ)
            .put(CjTokens.GT, OperatorNameConventions.COMPARE_GT)
            .put(CjTokens.LT, OperatorNameConventions.COMPARE_LT)
            .put(CjTokens.EXCLEQ, OperatorNameConventions.NOT_EQUALS)
            .put(CjTokens.EQEQ, OperatorNameConventions.EQUALS)
            .build()

    @JvmField
    val BOOLEAN_OPERATIONS_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.ANDAND, OperatorNameConventions.ANDAND)
            .put(CjTokens.OROR, OperatorNameConventions.OROR)
            .build()

    fun getNameForOperationSymbol(
        token: CjToken,
        unaryOperations: Boolean,
        binaryOperations: Boolean,
    ): Name? {
        var name: Name?

        if (binaryOperations) {
            name = BINARY_OPERATION_NAMES[token]
            if (name != null) return name
        }
        if (binaryOperations) {
            name = FLOW_OPERATION_NAMES[token]
            if (name != null) return name
        }
        if (unaryOperations) {
            name = UNARY_OPERATION_NAMES[token]
            if (name != null) return name
        }

        name = ASSIGNMENT_OPERATIONS[token]
        if (name != null) return name
        name = COMPARISON_OPERATIONS_NAMES[token]
        if (name != null) return name
        name = BOOLEAN_OPERATIONS_NAMES[token]
        if (name != null) return name
//        if (OperatorConventions.EQUALS_OPERATIONS.contains(token)) return EQUALS
//        if (OperatorConventions.IN_OPERATIONS.contains(token)) return CONTAINS
        return null
    }

    @JvmField
    val UNARY_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.EXCL, OperatorNameConventions.NOT)
            .put(CjTokens.PLUSPLUS, OperatorNameConventions.INC)
            .put(CjTokens.MINUSMINUS, OperatorNameConventions.DEC)
            .put(CjTokens.MINUS, OperatorNameConventions.UNARY_MINUS)
            .build()

    @JvmField
    val ASSIGNMENT_OPERATIONS: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.MULTEQ, OperatorNameConventions.TIMES_ASSIGN)
            .put(CjTokens.DIVEQ, OperatorNameConventions.DIV_ASSIGN)
            .put(CjTokens.PERCEQ, OperatorNameConventions.REM_ASSIGN)
            .put(CjTokens.PLUSEQ, OperatorNameConventions.PLUS_ASSIGN)
            .put(CjTokens.MINUSEQ, OperatorNameConventions.MINUS_ASSIGN)
            .put(CjTokens.MULMULEQ, OperatorNameConventions.EXPONENTIATION_ASSIGN)
            .put(CjTokens.OROREQ, OperatorNameConventions.OROREQ_ASSIGN)
            .put(CjTokens.ANDANDEQ, OperatorNameConventions.ANDANDEQ_ASSIGN)
            .put(CjTokens.OREQ, OperatorNameConventions.OREQ_ASSIGN)
            .put(CjTokens.ANDEQ, OperatorNameConventions.ANDEQ_ASSIGN)
            .put(CjTokens.XOREQ, OperatorNameConventions.XOREQ_ASSIGN)
            .put(CjTokens.GTGTEQ, OperatorNameConventions.GTGTEQ_ASSIGN)
            .put(CjTokens.LTLTEQ, OperatorNameConventions.LTLTEQ_ASSIGN)
            .build()

    @JvmField
    val FLOW_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.PIPELINE, OperatorNameConventions.PIPELINE)
            .put(CjTokens.COMPOSITION, OperatorNameConventions.COMPOSITION)
            .build()

    @JvmField
    val BINARY_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.MUL, OperatorNameConventions.TIMES)
            .put(CjTokens.MULMUL, OperatorNameConventions.EXPONENTIATION)
            .put(CjTokens.PLUS, OperatorNameConventions.PLUS)
            .put(CjTokens.MINUS, OperatorNameConventions.MINUS)
            .put(CjTokens.DIV, OperatorNameConventions.DIV)
            .put(CjTokens.PERC, OperatorNameConventions.REM)
            .put(CjTokens.GTGT, OperatorNameConventions.RIGHT_SHIFT)
            .put(CjTokens.LTLT, OperatorNameConventions.LEFT_SHIFT)
            .put(CjTokens.AND, OperatorNameConventions.AND)
            .put(CjTokens.XOR, OperatorNameConventions.XOR)
            .put(CjTokens.OR, OperatorNameConventions.OR)
            .build()
    // If you add new unary, binary or assignment operators, add it to OperatorConventionNames as well

    @JvmField
    val CONVENTION_NAMES: ImmutableSet<Name> =
        ImmutableSet.builder<Name>()
            .add(OperatorNameConventions.INVOKE)
            .add(OperatorNameConventions.GET)
            .addAll(UNARY_OPERATION_NAMES.values)
            .addAll(COMPARISON_OPERATIONS_NAMES.values)
            .addAll(BINARY_OPERATION_NAMES.values)
//            .addAll(ASSIGNMENT_OPERATIONS.values)
            .build()
}