package com.linqingying.cangjie.ide.refactoring

import com.linqingying.cangjie.psi.CjCallExpression
import com.linqingying.cangjie.psi.CjLambdaExpression
import com.linqingying.cangjie.psi.psiUtil.unpackFunctionLiteral

fun CjCallExpression.getLastLambdaExpression(): CjLambdaExpression? {
    if (lambdaArguments.isNotEmpty()) return null
    return valueArguments.lastOrNull()?.getArgumentExpression()?.unpackFunctionLiteral()
}
