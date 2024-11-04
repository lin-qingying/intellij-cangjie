package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.psi.ValueArgument
import com.linqingying.cangjie.resolve.calls.inference.model.ResolvedValueArgument

class DefaultValueArgument internal constructor() : ResolvedValueArgument {
    override val arguments: List<ValueArgument>
        get() = emptyList()

    override fun toString(): String {
        return "|DEFAULT|"
    }

    companion object {
        val DEFAULT: DefaultValueArgument = DefaultValueArgument()
    }
}
