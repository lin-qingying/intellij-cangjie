package com.linqingying.cangjie.resolve.calls

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjQualifiedExpression
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
