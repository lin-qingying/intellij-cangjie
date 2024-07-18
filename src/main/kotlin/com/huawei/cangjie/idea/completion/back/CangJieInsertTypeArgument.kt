package com.huawei.cangjie.idea.completion.back

import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getQualifiedExpressionForSelector
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder


data class TypeArgsWithOffset(val args: CjTypeArgumentList, val offset: Int)
var UserDataHolder.argList: TypeArgsWithOffset? by UserDataProperty(Key("CangJieInsertTypeArgument.ARG_LIST"))







private fun CjExpression.getPreviousInQualifiedChain(): CjExpression? {
    val receiverExpression = getQualifiedExpressionForSelector()?.receiverExpression
    return (receiverExpression as? CjQualifiedExpression)?.selectorExpression ?: receiverExpression
}

private fun CjExpression.findLastCallExpression() =
    ((this as? CjQualifiedExpression)?.selectorExpression ?: this) as? CjCallExpression
