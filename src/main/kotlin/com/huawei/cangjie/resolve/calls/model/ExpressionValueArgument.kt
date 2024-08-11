package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.psi.ValueArgument
import com.huawei.cangjie.resolve.calls.inference.model.ResolvedValueArgument

class ExpressionValueArgument(valueArgument:  ValueArgument?) :
    ResolvedValueArgument {
      val valueArgument:  ValueArgument? = valueArgument



    override val arguments: List<ValueArgument>
        get() {
            if (valueArgument == null) return emptyList()
            return listOf(valueArgument)
        }

    override fun toString(): String {
        val expression = valueArgument?.getArgumentExpression()
        return if (expression == null) "no expression" else expression.getText()
    }
}

