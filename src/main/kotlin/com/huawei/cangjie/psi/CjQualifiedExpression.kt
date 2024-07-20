package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.getElementTextWithContext
import com.huawei.cangjie.psi.psiUtil.siblings
import com.huawei.cangjie.utils.firstIsInstanceOrNull
import com.intellij.lang.ASTNode
import java.util.*



interface CjQualifiedExpression : CjExpression {
    val receiverExpression: CjExpression
        get() = getExpression(false) ?: throw AssertionError("No receiver found: ${getElementTextWithContext()}")

    val selectorExpression: CjExpression?
        get() = getExpression(true)

    val operationTokenNode: ASTNode
        get() = node.findChildByType(CjTokens.OPERATIONS) ?: error(
            "No operation node for ${node.elementType}. Children: ${Arrays.toString(children)}"
        )

    val operationSign: CjSingleValueToken
        get() = operationTokenNode.elementType as CjSingleValueToken

    private fun getExpression(afterOperation: Boolean): CjExpression? {
        return operationTokenNode.psi?.siblings(afterOperation, false)?.firstIsInstanceOrNull<CjExpression>()
    }
}
