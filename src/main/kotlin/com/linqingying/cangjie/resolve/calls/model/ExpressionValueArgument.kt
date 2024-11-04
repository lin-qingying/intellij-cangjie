package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.psi.ValueArgument
import com.linqingying.cangjie.resolve.calls.inference.model.ResolvedValueArgument

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

