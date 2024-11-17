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

package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.createTupleType
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.psi.CjTupleExpression
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.types.util.TypeUtils.noExpectedType
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo

class TuplesTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    override fun visitTupleExpression(expression: CjTupleExpression, data: ExpressionTypingContext): CangJieTypeInfo {


//        需要做什么？
//       将元组内表达式遍历类型

//        是否应该使用元组的第一个类型？ 考虑声明类型非元组类型的情况
        val types = mutableListOf<CangJieType>()
        val expressions = expression.expressions
        for (i in expressions.indices) {

            val expectedType =    if (!noExpectedType(data.expectedType) && CangJieBuiltIns.isTuple(data.expectedType) && i < data.expectedType.arguments.size) {
                    data.expectedType.arguments[i].type

                } else {
                    NO_EXPECTED_TYPE
                }

            facade.getTypeInfo(
                expressions[i],
                data.replaceExpectedType(
                    expectedType

                )
            ).type?.let {
                // 如果存在预期类型且实际类型是其子类型，则使用预期类型；否则使用实际类型

                if (!noExpectedType(expectedType) && components.cangjieTypeChecker.isSubtypeOf(it,expectedType  )) {
//        为Option装箱 为Tuple特殊处理
                    types.add(expectedType)
                } else {
                    types.add(it)
                }

            }
        }


        val tupleType = createTupleType(
            components.builtIns, Annotations.EMPTY, types
        )

        return components.dataFlowAnalyzer.createCheckedTypeInfo(tupleType, data, expression)

    }
}
