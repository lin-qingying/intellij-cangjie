package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.createTupleType
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.psi.CjTupleExpression
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.huawei.cangjie.types.util.TypeUtils.noExpectedType
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo

class TuplesTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    override fun visitTupleExpression(expression: CjTupleExpression, data: ExpressionTypingContext): CangJieTypeInfo {


//        需要做什么？
//       将元组内表达式遍历类型

//        是否应该使用元组的第一个类型？ 考虑声明类型非元组类型的情况
        val types = mutableListOf<CangJieType>()
        val expressions = expression.expressions
        for (i in expressions.indices) {

            val expectedType =
                if (!noExpectedType(data.expectedType) && CangJieBuiltIns.isTuple(data.expectedType) && i < data.expectedType.arguments.size) {
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

                if (!noExpectedType(data.expectedType) && components.cangjieTypeChecker.isSubtypeOf(it,expectedType  )) {
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
