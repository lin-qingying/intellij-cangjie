package com.huawei.cangjie.resolve.calls

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjQualifiedExpression
import com.intellij.lang.ASTNode

data class CallExpressionElement internal constructor(val qualified: CjQualifiedExpression) {

    val receiver: CjExpression
        get() = qualified.receiverExpression

    val selector: CjExpression?
        get() = qualified.selectorExpression

    val safe: Boolean
        get() = qualified.operationSign == CjTokens.SAFE_ACCESS

    val node: ASTNode
        get() = qualified.operationTokenNode
}
fun unrollToLeftMostQualifiedExpression(expression: CjQualifiedExpression): List<CjQualifiedExpression> {
    val unrolled = arrayListOf<CjQualifiedExpression>()

    var finger = expression
    while (true) {
        unrolled.add(finger)
        val receiver = finger.receiverExpression
        if (receiver !is CjQualifiedExpression) {
            break
        }
        finger = receiver
    }

    return unrolled.asReversed()
}
