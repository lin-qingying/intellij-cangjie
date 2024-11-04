package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode



abstract class CjWhileExpressionBase(node: ASTNode) : CjLoopExpression(node) {
    @get: IfNotParsed
    val condition: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.CONDITION)
}

