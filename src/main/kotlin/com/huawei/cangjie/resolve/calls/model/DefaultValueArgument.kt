package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.psi.ValueArgument
import com.huawei.cangjie.resolve.calls.inference.model.ResolvedValueArgument

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
