package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.psi.ValueArgument
import com.huawei.cangjie.resolve.calls.inference.model.ResolvedValueArgument


class VarargValueArgument(
    arguments: List<ValueArgument> = emptyList()
) : ResolvedValueArgument {
    override val arguments = mutableListOf<ValueArgument>()

    init {
        this.arguments.addAll(arguments)
    }


    fun addArgument(argument: ValueArgument) {
        arguments.add(argument)
    }


    override fun toString(): String {
        val builder = StringBuilder("vararg:{")
        val iterator: Iterator<ValueArgument> = arguments.iterator()
        while (iterator.hasNext()) {
            val valueArgument: ValueArgument = iterator.next()
            val expression = valueArgument.getArgumentExpression()
            builder.append(if (expression == null) "no expression" else expression.text)
            if (iterator.hasNext()) {
                builder.append(", ")
            }
        }
        return builder.append("}").toString()
    }
}
