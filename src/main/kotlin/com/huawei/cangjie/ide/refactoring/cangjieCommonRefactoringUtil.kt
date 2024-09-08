package com.huawei.cangjie.ide.refactoring

import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjLambdaExpression
import com.huawei.cangjie.psi.psiUtil.unpackFunctionLiteral

fun CjCallExpression.getLastLambdaExpression(): CjLambdaExpression? {
    if (lambdaArguments.isNotEmpty()) return null
    return valueArguments.lastOrNull()?.getArgumentExpression()?.unpackFunctionLiteral()
}
