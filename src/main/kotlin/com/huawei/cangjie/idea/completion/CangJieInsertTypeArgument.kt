package com.huawei.cangjie.idea.completion

import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getQualifiedExpressionForSelector
import com.huawei.cangjie.utils.match
import com.huawei.cangjie.utils.parents
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil


data class TypeArgsWithOffset(val args: CjTypeArgumentList, val offset: Int)
var UserDataHolder.argList: TypeArgsWithOffset? by UserDataProperty(Key("KotlinInsertTypeArgument.ARG_LIST"))







private fun CjExpression.getPreviousInQualifiedChain(): CjExpression? {
    val receiverExpression = getQualifiedExpressionForSelector()?.receiverExpression
    return (receiverExpression as? CjQualifiedExpression)?.selectorExpression ?: receiverExpression
}

private fun CjExpression.findLastCallExpression() =
    ((this as? CjQualifiedExpression)?.selectorExpression ?: this) as? CjCallExpression
