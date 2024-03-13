package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

class CjSliceExpression(node: ASTNode):CjUnaryExpression(node) {
    override fun getBaseExpression(): CjExpression? {
        return PsiTreeUtil.getPrevSiblingOfType(operationReference, CjExpression::class.java)

    }
}