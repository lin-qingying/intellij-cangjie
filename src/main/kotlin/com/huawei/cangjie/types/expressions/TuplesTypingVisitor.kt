package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.psi.CjTupleExpression
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo

class TuplesTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    override fun visitTupleExpression(expression: CjTupleExpression, data: ExpressionTypingContext?): CangJieTypeInfo {


//        需要做什么？
//       将元组内表达式遍历类型

//        是否应该使用元组的第一个类型？ 考虑声明类型非元组类型的情况
        expression.expressions.forEach {
            facade.getTypeInfo(it,data)
        }
        return super.visitTupleExpression(expression, data)
    }
}
