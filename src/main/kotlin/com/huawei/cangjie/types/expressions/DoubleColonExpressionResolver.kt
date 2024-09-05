package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.psi.CjCallExpression


// Returns true if this expression has the form "A<B>" which means it's a type on the LHS of a double colon expression
internal val CjCallExpression.isWithoutValueArguments: Boolean
    get() = valueArgumentList == null && lambdaArguments.isEmpty()

