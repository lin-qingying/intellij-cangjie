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

package cn.cangnova.cangjie.utils.exceptions

import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableSet
import cn.cangnova.cangjie.lexer.CjSingleValueToken
import cn.cangnova.cangjie.lexer.CjToken
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.utils.OperatorNameConventions.AND
import cn.cangnova.cangjie.utils.OperatorNameConventions.ANDAND
import cn.cangnova.cangjie.utils.OperatorNameConventions.ANDANDEQ_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.ANDEQ_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.COMPARE_GT
import cn.cangnova.cangjie.utils.OperatorNameConventions.COMPARE_GTEQ
import cn.cangnova.cangjie.utils.OperatorNameConventions.COMPARE_LT
import cn.cangnova.cangjie.utils.OperatorNameConventions.COMPARE_LTEQ
import cn.cangnova.cangjie.utils.OperatorNameConventions.DEC
import cn.cangnova.cangjie.utils.OperatorNameConventions.DIV
import cn.cangnova.cangjie.utils.OperatorNameConventions.DIV_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.EQUALS
import cn.cangnova.cangjie.utils.OperatorNameConventions.EXPONENTIATION
import cn.cangnova.cangjie.utils.OperatorNameConventions.EXPONENTIATION_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.GET
import cn.cangnova.cangjie.utils.OperatorNameConventions.GTGTEQ_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.INC
import cn.cangnova.cangjie.utils.OperatorNameConventions.INVOKE
import cn.cangnova.cangjie.utils.OperatorNameConventions.LEFT_SHIFT
import cn.cangnova.cangjie.utils.OperatorNameConventions.LTLTEQ_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.MINUS
import cn.cangnova.cangjie.utils.OperatorNameConventions.MINUS_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.NOT
import cn.cangnova.cangjie.utils.OperatorNameConventions.NOT_EQUALS
import cn.cangnova.cangjie.utils.OperatorNameConventions.OR
import cn.cangnova.cangjie.utils.OperatorNameConventions.OREQ_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.OROR
import cn.cangnova.cangjie.utils.OperatorNameConventions.OROREQ_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.PLUS
import cn.cangnova.cangjie.utils.OperatorNameConventions.PLUS_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.REM
import cn.cangnova.cangjie.utils.OperatorNameConventions.REM_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.RIGHT_SHIFT
import cn.cangnova.cangjie.utils.OperatorNameConventions.TIMES
import cn.cangnova.cangjie.utils.OperatorNameConventions.TIMES_ASSIGN
import cn.cangnova.cangjie.utils.OperatorNameConventions.UNARY_MINUS
import cn.cangnova.cangjie.utils.OperatorNameConventions.XOR
import cn.cangnova.cangjie.utils.OperatorNameConventions.XOREQ_ASSIGN
import com.intellij.psi.tree.IElementType
import cn.cangnova.cangjie.utils.OperatorNameConventions.COMPOSITION
import cn.cangnova.cangjie.utils.OperatorNameConventions.PIPELINE


object OperatorConventions {

    val DOUBLE: Name = Name.identifier("toDouble")

    val FLOAT: Name = Name.identifier("toFloat")

    val LONG: Name = Name.identifier("toLong")

    val INT: Name = Name.identifier("toInt")

    val CHAR: Name = Name.identifier("toChar")

    val SHORT: Name = Name.identifier("toShort")

    val BYTE: Name = Name.identifier("toByte")

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
            LONG,
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
                BINARY_OPERATION_NAMES.containsKey(type) ||FLOW_OPERATION_NAMES.containsKey(type) ||
                COMPARISON_OPERATIONS_NAMES.containsKey(
                    type
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
            .put(CjTokens.GTEQ, COMPARE_GTEQ)
            .put(CjTokens.LTEQ, COMPARE_LTEQ)
            .put(CjTokens.GT, COMPARE_GT)
            .put(CjTokens.LT, COMPARE_LT)
            .put(CjTokens.EXCLEQ, NOT_EQUALS)
            .put(CjTokens.EQEQ, EQUALS)
            .build()

    @JvmField
    val BOOLEAN_OPERATIONS_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.ANDAND, ANDAND)
            .put(CjTokens.OROR, OROR)
            .build()

    fun getNameForOperationSymbol(
        token: CjToken,
        unaryOperations: Boolean,
        binaryOperations: Boolean
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
            .put(CjTokens.EXCL, NOT)
            .put(CjTokens.PLUSPLUS, INC)
            .put(CjTokens.MINUSMINUS, DEC)
            .put(CjTokens.MINUS, UNARY_MINUS)

            .build()

    @JvmField
    val ASSIGNMENT_OPERATIONS: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.MULTEQ, TIMES_ASSIGN)
            .put(CjTokens.DIVEQ, DIV_ASSIGN)
            .put(CjTokens.PERCEQ, REM_ASSIGN)
            .put(CjTokens.PLUSEQ, PLUS_ASSIGN)
            .put(CjTokens.MINUSEQ, MINUS_ASSIGN)
            .put(CjTokens.MULMULEQ, EXPONENTIATION_ASSIGN)


            .put(CjTokens.OROREQ, OROREQ_ASSIGN)
            .put(CjTokens.ANDANDEQ, ANDANDEQ_ASSIGN)
            .put(CjTokens.OREQ, OREQ_ASSIGN)
            .put(CjTokens.ANDEQ, ANDEQ_ASSIGN)
            .put(CjTokens.XOREQ, XOREQ_ASSIGN)
            .put(CjTokens.GTGTEQ, GTGTEQ_ASSIGN)
            .put(CjTokens.LTLTEQ, LTLTEQ_ASSIGN)

            .build()

    @JvmField
    val FLOW_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.PIPELINE, PIPELINE)
            .put(CjTokens.COMPOSITION, COMPOSITION)



            .build()
    @JvmField
    val BINARY_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.MUL, TIMES)
            .put(CjTokens.MULMUL, EXPONENTIATION)
            .put(CjTokens.PLUS, PLUS)
            .put(CjTokens.MINUS, MINUS)
            .put(CjTokens.DIV, DIV)
            .put(CjTokens.PERC, REM)

            .put(CjTokens.GTGT, RIGHT_SHIFT)
            .put(CjTokens.LTLT, LEFT_SHIFT)

            .put(CjTokens.AND, AND)
            .put(CjTokens.XOR, XOR)
            .put(CjTokens.OR, OR)


            .build()
    // If you add new unary, binary or assignment operators, add it to OperatorConventionNames as well

    @JvmField
    val CONVENTION_NAMES: ImmutableSet<Name> =
        ImmutableSet.builder<Name>()
            .add(INVOKE)

            .add(GET)
            .addAll(UNARY_OPERATION_NAMES.values)
            .addAll(COMPARISON_OPERATIONS_NAMES.values)
            .addAll(BINARY_OPERATION_NAMES.values)
//            .addAll(ASSIGNMENT_OPERATIONS.values)
            .build()

}
