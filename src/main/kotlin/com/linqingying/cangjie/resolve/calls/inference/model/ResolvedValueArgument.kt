package com.linqingying.cangjie.resolve.calls.inference.model

import com.linqingying.cangjie.psi.ValueArgument

interface ResolvedValueArgument {
    val arguments: List<ValueArgument>
}
